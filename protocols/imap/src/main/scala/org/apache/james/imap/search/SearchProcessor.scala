/** **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                 *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.imap.search

import java.io.Closeable
import java.util

import org.apache.james.imap.api.display.HumanReadableText
import org.apache.james.imap.api.message.request.SearchResultOption
import org.apache.james.imap.api.message.response.StatusResponseFactory
import org.apache.james.imap.api.message.{Capability, IdRange, UidRange}
import org.apache.james.imap.api.process.{ImapProcessor, ImapSession, SearchResUtil, SelectedMailbox}
import org.apache.james.imap.message.response.ESearchResponse
import org.apache.james.imap.processor.{AbstractMailboxProcessor, CapabilityImplementingProcessor}
import org.apache.james.mailbox.exception.{MailboxException, MessageRangeException}
import org.apache.james.mailbox.model.{FetchGroup, MessageRange, SearchQuery}
import org.apache.james.mailbox.{MailboxManager, MailboxSession, MessageManager, MessageUid, ModSeq}
import org.apache.james.metrics.api.MetricFactory
import org.apache.james.util.MDCBuilder
import org.slf4j.LoggerFactory

object SearchProcessor {
  private val LOGGER = LoggerFactory.getLogger(classOf[SearchProcessor])
  private val SEARCH_MODSEQ = "SEARCH_MODSEQ"
  private val CAPS =
    Seq(
      Capability.of("WITHIN"),
      Capability.of("ESEARCH"),
      Capability.of("SEARCHRES"))
}

class SearchProcessor(val next: ImapProcessor, val mailboxManager: MailboxManager, val factory: StatusResponseFactory, val metricFactory: MetricFactory) extends AbstractMailboxProcessor[SearchRequest](classOf[SearchRequest], next, mailboxManager, factory, metricFactory) with CapabilityImplementingProcessor {
  override protected def processRequest(request: SearchRequest, session: ImapSession, responder: ImapProcessor.Responder): Unit = {
    val operation = request.operation
    val searchKey = operation.key
    val useUids = request.useUids
    val resultOptions = operation.options
    try {
      val mailbox = getSelectedMailbox(session)
      val query = SearchQueryConverter.toQuery(searchKey, session)
      val msession = session.getMailboxSession
      val uids = performUidSearch(mailbox, query, msession)
      val ids: Seq[Long] = asResults(session, useUids, uids)
      // Check if the search did contain the MODSEQ searchkey. If so we need to include the highest mod in the response.
      //
      // See RFC4551: 3.4. MODSEQ Search Criterion in SEARCH
      val highestModSeq =
      if (session.getAttribute(SearchProcessor.SEARCH_MODSEQ) != null) {
        val metaData = mailbox.getMetaData(false, msession, MessageManager.MetaData.FetchGroup.NO_COUNT)
        import scala.jdk.CollectionConverters._
        val result = findHighestModSeq(msession, mailbox, MessageRange.toRanges(uids.asJavaCollection).asScala.toSeq, metaData.getHighestModSeq)
        // Enable CONDSTORE as this is a CONDSTORE enabling command
        condstoreEnablingCommand(session, responder, metaData, true)
        result
      }
      else null
      val response =
        if (resultOptions == null || resultOptions.isEmpty) SearchResponse(ids, highestModSeq)
        else {
          import scala.jdk.CollectionConverters._
          val idRanges = IdRange.mergeRanges(ids.map(new IdRange(_)).asJava).asScala
          val uidRanges = UidRange.mergeRanges(uids.map(new UidRange(_)).asJava).asScala
          val esearch = resultOptions.exists(SearchResultOption.SAVE != _)
          if (esearch) {
            val min = ids.headOption.getOrElse(-1L)
            val max = ids.lastOption.getOrElse(-1L)
            val count = ids.length
            // Save the sequence-set for later usage. This is part of SEARCHRES
            if (resultOptions.contains(SearchResultOption.SAVE)) {
              val toSave: Iterable[IdRange] =
                if (resultOptions.contains(SearchResultOption.ALL) || resultOptions.contains(SearchResultOption.COUNT)) {
                  // if the options contain ALL or COUNT we need to save the complete sequence-set
                  idRanges
                } else {
                  Seq(
                      if (resultOptions.contains(SearchResultOption.MIN)) Some(new IdRange(min))
                      else None,
                      if (resultOptions.contains(SearchResultOption.MAX)) Some(new IdRange(max))
                      else None)
                    .flatten
                  }
              SearchResUtil.saveSequenceSet(session, toSave.toArray)
            }
            new ESearchResponse(min, max, count, idRanges.toArray, uidRanges.toArray, highestModSeq, request.getTag, useUids, resultOptions.asJava)

          } else {
            // Just save the returned sequence-set as this is not SEARCHRES + ESEARCH
            SearchResUtil.saveSequenceSet(session, idRanges.toArray)
            SearchResponse(ids, highestModSeq)
          }
        }
      responder.respond(response)
      val omitExpunged = !useUids
      unsolicitedResponses(session, responder, omitExpunged, useUids)
      okComplete(request, responder)
    } catch {
      case e: MessageRangeException =>
        SearchProcessor.LOGGER.debug("Search failed in mailbox {} because of an invalid sequence-set ", session.getSelected.getMailboxId, e)
        taggedBad(request, responder, HumanReadableText.INVALID_MESSAGESET)
      case e: MailboxException =>
        SearchProcessor.LOGGER.error("Search failed in mailbox {}", session.getSelected.getMailboxId, e)
        no(request, responder, HumanReadableText.SEARCH_FAILED)
        if (resultOptions.contains(SearchResultOption.SAVE)) { // Reset the saved sequence-set on a BAD response if the SAVE option was used.
          // See RFC5182 2.1.Normative Description of the SEARCHRES Extension
          SearchResUtil.resetSavedSequenceSet(session)
        }
    } finally session.setAttribute(SearchProcessor.SEARCH_MODSEQ, null)
  }

  private def asResults(session: ImapSession, useUids: Boolean, uids: Seq[MessageUid]) =
    if (useUids)
      uids.map(_.asLong)
    else
      uids.map(session.getSelected.msn(_))
        .filter(_ != SelectedMailbox.NO_SUCH_MESSAGE)
        .map(_.longValue)

  @throws[MailboxException]
  private def performUidSearch(mailbox: MessageManager, query: SearchQuery, msession: MailboxSession) = try {
    import scala.jdk.StreamConverters._
    val stream = mailbox.search(query, msession)
    try stream.toScala(Seq)
    finally if (stream != null) stream.close()
  }

  /**
   * Find the highest mod-sequence number in the given {@link MessageRange}'s.
   */
  @throws[MailboxException]
  private def findHighestModSeq(session: MailboxSession, mailbox: MessageManager, ranges: Seq[MessageRange], currentHighest: ModSeq): ModSeq = {
    var highestModSeq: ModSeq = null
    // Reverse loop over the ranges as its more likely that we find a match at the end
    for (range <- ranges.reverse) {
      val results = mailbox.getMessages(range, FetchGroup.MINIMAL, session)
      while (results.hasNext) {
        val modSeq = results.next.getModSeq
        highestModSeq =
          if (highestModSeq == null || modSeq.asLong > highestModSeq.asLong)
            modSeq
          else
            highestModSeq
        if (highestModSeq.equals(currentHighest)) return highestModSeq
      }
    }
    highestModSeq
  }

  override def getImplementedCapabilities(session: ImapSession): util.List[Capability] = {
    import scala.jdk.CollectionConverters._
    SearchProcessor.CAPS.asJava
  }

  override protected def addContextToMDC(request: SearchRequest): Closeable =
    MDCBuilder.create
      .addContext(MDCBuilder.ACTION, "SEARCH")
      .addContext("useUid", request.useUids)
      .addContext("searchOperation", request.operation)
      .build()
}