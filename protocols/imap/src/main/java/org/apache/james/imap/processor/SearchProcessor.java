/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.imap.processor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.Capability;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.UidRange;
import org.apache.james.imap.api.message.request.Criterion$;
import org.apache.james.imap.api.message.request.SearchKey;
import org.apache.james.imap.api.message.request.SearchOperation;
import org.apache.james.imap.api.message.request.SearchResultOption;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SearchResUtil;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.SearchRequest;
import org.apache.james.imap.message.response.ESearchResponse;
import org.apache.james.imap.message.response.SearchResponse;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageManager.MetaData;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.ModSeq;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MessageRangeException;
import org.apache.james.mailbox.model.FetchGroup;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.MessageResultIterator;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.util.MDCBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

public class SearchProcessor extends AbstractMailboxProcessor<SearchRequest> implements CapabilityImplementingProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchProcessor.class);

    static final String SEARCH_MODSEQ = "SEARCH_MODSEQ";
    private static final List<Capability> CAPS = ImmutableList.of(Capability.of("WITHIN"), Capability.of("ESEARCH"), Capability.of("SEARCHRES"));
    
    public SearchProcessor(ImapProcessor next, MailboxManager mailboxManager, StatusResponseFactory factory,
            MetricFactory metricFactory) {
        super(SearchRequest.class, next, mailboxManager, factory, metricFactory);
    }

    @Override
    protected void processRequest(SearchRequest request, ImapSession session, Responder responder) {
        final SearchOperation operation = request.getSearchOperation();
        final SearchKey searchKey = operation.getSearchKey();
        final boolean useUids = request.isUseUids();
        List<SearchResultOption> resultOptions = operation.getResultOptions();

        try {

            final MessageManager mailbox = getSelectedMailbox(session);

            final SearchQuery query = Criterion$.MODULE$.toQuery(searchKey, session);
            MailboxSession msession = session.getMailboxSession();

            final Collection<MessageUid> uids = performUidSearch(mailbox, query, msession);
            final Collection<Long> results = asResults(session, useUids, uids);

            // Check if the search did contain the MODSEQ searchkey. If so we need to include the highest mod in the response.
            //
            // See RFC4551: 3.4. MODSEQ Search Criterion in SEARCH
            final ModSeq highestModSeq;
            if (session.getAttribute(SEARCH_MODSEQ) != null) {
                MetaData metaData = mailbox.getMetaData(false, msession, MessageManager.MetaData.FetchGroup.NO_COUNT);
                highestModSeq = findHighestModSeq(msession, mailbox, MessageRange.toRanges(uids), metaData.getHighestModSeq());
                
                // Enable CONDSTORE as this is a CONDSTORE enabling command
                condstoreEnablingCommand(session, responder,  metaData, true);                
                
            } else {
                highestModSeq = null;
            }
            final long[] ids = toArray(results);

            final ImapResponseMessage response;
            if (resultOptions == null || resultOptions.isEmpty()) {
                response = new SearchResponse(ids, highestModSeq);
            } else {
                List<Long> idList = new ArrayList<>(ids.length);
                for (long id : ids) {
                    idList.add(id);
                }

                List<IdRange> idsAsRanges = new ArrayList<>();
                for (Long id: idList) {
                    idsAsRanges.add(new IdRange(id));
                }
                IdRange[] idRanges = IdRange.mergeRanges(idsAsRanges).toArray(new IdRange[0]);
                
                List<UidRange> uidsAsRanges = new ArrayList<>();
                for (MessageUid uid: uids) {
                    uidsAsRanges.add(new UidRange(uid));
                }
                UidRange[] uidRanges = UidRange.mergeRanges(uidsAsRanges).toArray(new UidRange[0]);
                
                boolean esearch = false;
                for (SearchResultOption resultOption : resultOptions) {
                    if (SearchResultOption.SAVE != resultOption) {
                        esearch = true;
                        break;
                    }
                }
                
                if (esearch) {
                    long min = -1;
                    long max = -1;
                    long count = ids.length;

                    if (ids.length > 0) {
                        min = ids[0];
                        max = ids[ids.length - 1];
                    } 
                   
                    
                    // Save the sequence-set for later usage. This is part of SEARCHRES 
                    if (resultOptions.contains(SearchResultOption.SAVE)) {
                        if (resultOptions.contains(SearchResultOption.ALL) || resultOptions.contains(SearchResultOption.COUNT)) {
                            // if the options contain ALL or COUNT we need to save the complete sequence-set
                            SearchResUtil.saveSequenceSet(session, idRanges);
                        } else {
                            List<IdRange> savedRanges = new ArrayList<>();
                            if (resultOptions.contains(SearchResultOption.MIN)) {
                                // Store the MIN
                                savedRanges.add(new IdRange(min));  
                            } 
                            if (resultOptions.contains(SearchResultOption.MAX)) {
                                // Store the MAX
                                savedRanges.add(new IdRange(max));
                            }
                            SearchResUtil.saveSequenceSet(session, savedRanges.toArray(new IdRange[0]));
                        }
                    }
                    response = new ESearchResponse(min, max, count, idRanges, uidRanges, highestModSeq, request.getTag(), useUids, resultOptions);
                } else {
                    // Just save the returned sequence-set as this is not SEARCHRES + ESEARCH
                    SearchResUtil.saveSequenceSet(session, idRanges);
                    response = new SearchResponse(ids, highestModSeq);

                }
            }

            responder.respond(response);

            boolean omitExpunged = (!useUids);
            unsolicitedResponses(session, responder, omitExpunged, useUids);
            okComplete(request, responder);
        } catch (MessageRangeException e) {
            LOGGER.debug("Search failed in mailbox {} because of an invalid sequence-set ", session.getSelected().getMailboxId(), e);
            taggedBad(request, responder, HumanReadableText.INVALID_MESSAGESET);
        } catch (MailboxException e) {
            LOGGER.error("Search failed in mailbox {}", session.getSelected().getMailboxId(), e);
            no(request, responder, HumanReadableText.SEARCH_FAILED);
            
            if (resultOptions.contains(SearchResultOption.SAVE)) {
                // Reset the saved sequence-set on a BAD response if the SAVE option was used.
                //
                // See RFC5182 2.1.Normative Description of the SEARCHRES Extension
                SearchResUtil.resetSavedSequenceSet(session);
            }
        } finally {
            session.setAttribute(SEARCH_MODSEQ, null);
        }
    }

    private Collection<Long> asResults(ImapSession session, boolean useUids, Collection<MessageUid> uids) {
        if (useUids) {
            return uids.stream()
                .map(MessageUid::asLong)
                .collect(Guavate.toImmutableList());
        } else {
            return uids.stream()
                .map(uid -> session.getSelected().msn(uid))
                .map(Integer::longValue)
                .filter(msn -> msn != SelectedMailbox.NO_SUCH_MESSAGE)
                .collect(Guavate.toImmutableList());
        }
    }

    private Collection<MessageUid> performUidSearch(MessageManager mailbox, SearchQuery query, MailboxSession msession) throws MailboxException {
        try (Stream<MessageUid> stream = mailbox.search(query, msession)) {
            return stream.collect(Guavate.toImmutableList());
        }
    }

    private long[] toArray(Collection<Long> results) {
        return results.stream().mapToLong(x -> x).toArray();
    }

    /**
     * Find the highest mod-sequence number in the given {@link MessageRange}'s.
     * 
     * @param session
     * @param mailbox
     * @param ranges
     * @param currentHighest
     * @return highestModSeq
     * @throws MailboxException
     */
    private ModSeq findHighestModSeq(MailboxSession session, MessageManager mailbox, List<MessageRange> ranges, ModSeq currentHighest) throws MailboxException {
        ModSeq highestModSeq = null;
        
        // Reverse loop over the ranges as its more likely that we find a match at the end
        int size = ranges.size();
        for (int i = size - 1; i > 0; i--) {
            MessageResultIterator results = mailbox.getMessages(ranges.get(i), FetchGroup.MINIMAL, session);
            while (results.hasNext()) {
                ModSeq modSeq = results.next().getModSeq();
                if (highestModSeq == null || modSeq.asLong() > highestModSeq.asLong()) {
                    highestModSeq = modSeq;
                }
                if (highestModSeq == currentHighest) {
                    return highestModSeq;
                }
            }
            
        }
        return highestModSeq;
    }

    @Override
    public List<Capability> getImplementedCapabilities(ImapSession session) {
        return CAPS;
    }

    @Override
    protected Closeable addContextToMDC(SearchRequest request) {
        return MDCBuilder.create()
            .addContext(MDCBuilder.ACTION, "SEARCH")
            .addContext("useUid", request.isUseUids())
            .addContext("searchOperation", request.getSearchOperation())
            .build();
    }
}
