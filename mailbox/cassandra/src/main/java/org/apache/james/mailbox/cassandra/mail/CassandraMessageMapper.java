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

package org.apache.james.mailbox.cassandra.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.backends.cassandra.utils.FunctionRunnerWithRetry;
import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.mail.utils.MessageDeletedDuringFlagsUpdateException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.FlagsUpdate;
import org.apache.james.mailbox.store.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class CassandraMessageMapper implements MessageMapper<CassandraId> {

    private final ModSeqProvider<CassandraId> modSeqProvider;
    private final MailboxSession mailboxSession;
    private final UidProvider<CassandraId> uidProvider;
    private final int maxRetries;
    private final CassandraMessageRepository messageRepository;
    private final CassandraMailboxCountersRepository mailboxCountersRepository;

    public CassandraMessageMapper(UidProvider<CassandraId> uidProvider, ModSeqProvider<CassandraId> modSeqProvider, MailboxSession mailboxSession, int maxRetries, CassandraMessageRepository messageRepository, CassandraMailboxCountersRepository mailboxCountersRepository) {
        this.uidProvider = uidProvider;
        this.modSeqProvider = modSeqProvider;
        this.mailboxSession = mailboxSession;
        this.maxRetries = maxRetries;
        this.messageRepository = messageRepository;
        this.mailboxCountersRepository = mailboxCountersRepository;
    }

    @Override
    public long countMessagesInMailbox(Mailbox<CassandraId> mailbox) throws MailboxException {
        return mailboxCountersRepository.countMessagesInMailbox(mailbox).join();
    }

    @Override
    public long countUnseenMessagesInMailbox(Mailbox<CassandraId> mailbox) throws MailboxException {
        return mailboxCountersRepository.countUnseenMessagesInMailbox(mailbox).join();
    }

    @Override
    public void delete(Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> message) {
        messageRepository.delete(mailbox, message);
        CompletableFuture
            .allOf(
                mailboxCountersRepository.decrementCount(mailbox),
                decrementUnseen(mailbox, message))
            .join();
    }

    private CompletableFuture<Void> decrementUnseen(Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> message) {
        if (!message.isSeen()) {
            return mailboxCountersRepository.decrementUnseen(mailbox);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Iterator<MailboxMessage<CassandraId>> findInMailbox(Mailbox<CassandraId> mailbox, MessageRange set, FetchType ftype, int max) throws MailboxException {
        return messageRepository.loadMessageRange(mailbox, set)
            .sorted()
            .iterator();
    }

    @Override
    public List<Long> findRecentMessageUidsInMailbox(Mailbox<CassandraId> mailbox) throws MailboxException {
        return messageRepository.findRecentMessageUids(mailbox)
            .sorted()
            .collect(Collectors.toList());
    }

    @Override
    public Long findFirstUnseenMessageUid(Mailbox<CassandraId> mailbox) throws MailboxException {
        return messageRepository.findUnseenMessageUids(mailbox)
            .sorted()
            .findFirst()
            .orElse(null);
    }

    @Override
    public Map<Long, MessageMetaData> expungeMarkedForDeletionInMailbox(final Mailbox<CassandraId> mailbox, MessageRange set) throws MailboxException {
        return messageRepository.findDeletedMessages(mailbox, set)
            .peek(message -> delete(mailbox, message))
            .collect(Collectors.toMap(MailboxMessage::getUid, SimpleMessageMetaData::new));
    }

    @Override
    public MessageMetaData move(Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> original) throws MailboxException {
        throw new UnsupportedOperationException("Not implemented - see https://issues.apache.org/jira/browse/IMAP-370");
    }

    @Override
    public void endRequest() {
        // Do nothing
    }

    @Override
    public long getHighestModSeq(Mailbox<CassandraId> mailbox) throws MailboxException {
        return modSeqProvider.highestModSeq(mailboxSession, mailbox);
    }

    @Override
    public MessageMetaData add(Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> message) throws MailboxException {
        message.setUid(uidProvider.nextUid(mailboxSession, mailbox));
        message.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
        MessageMetaData messageMetaData = messageRepository.save(mailbox, message);
        CompletableFuture
            .allOf(
                incrementUnseen(mailbox, message),
                mailboxCountersRepository.incrementCount(mailbox))
            .join();
        return messageMetaData;
    }

    private CompletableFuture<Void> incrementUnseen(Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> message) {
        if (!message.isSeen()) {
            return mailboxCountersRepository.incrementUnseen(mailbox);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Iterator<UpdatedFlags> updateFlags(Mailbox<CassandraId> mailbox, FlagsUpdate flagUpdate, MessageRange set) throws MailboxException {
        Function<UpdatedFlags, CompletableFuture<Iterable<UpdatedFlags>>> handleUnseenCountAsync = 
                flags -> manageUnseenMessageCounts(mailbox, flags).thenApply(ImmutableList::of);
                
        return messageRepository.loadMessageRange(mailbox, set)
            .map(message -> updateFlagsOnMessage(mailbox, flagUpdate, message))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(handleUnseenCountAsync)
            .reduce(this::combineFutures)
            .map(CompletableFuture::join)
            .orElseGet(ImmutableList::of)
            .iterator();
    }

    private <U> CompletableFuture<Iterable<U>> combineFutures(CompletableFuture<Iterable<U>> f1, CompletableFuture<Iterable<U>> f2) {
        return f1.thenCombine(f2, Iterables::concat);
    }
    
    @Override
    public <T> T execute(Transaction<T> transaction) throws MailboxException {
        return transaction.run();
    }

    @Override
    public MessageMetaData copy(Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> original) throws MailboxException {
        CompletableFuture<Void> countersUpdate = CompletableFuture.allOf(
            mailboxCountersRepository.incrementCount(mailbox),
            incrementUnseen(mailbox, original));
        original.setUid(uidProvider.nextUid(mailboxSession, mailbox));
        original.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
        original.setFlags(new FlagsBuilder().add(original.createFlags()).add(Flag.RECENT).build());
        countersUpdate.join();
        return messageRepository.save(mailbox, original);
    }

    @Override
    public long getLastUid(Mailbox<CassandraId> mailbox) throws MailboxException {
        return uidProvider.lastUid(mailboxSession, mailbox);
    }

    private CompletableFuture<UpdatedFlags> manageUnseenMessageCounts(Mailbox<CassandraId> mailbox, UpdatedFlags updatedFlags) {
        Flags oldFlags = updatedFlags.getOldFlags();
        Flags newFlags = updatedFlags.getNewFlags();
        if (oldFlags.contains(Flag.SEEN) && !newFlags.contains(Flag.SEEN)) {
            return mailboxCountersRepository.incrementUnseen(mailbox).thenApply(x -> updatedFlags);
        } else if (!oldFlags.contains(Flag.SEEN) && newFlags.contains(Flag.SEEN)) {
            return mailboxCountersRepository.decrementUnseen(mailbox).thenApply(x -> updatedFlags);
        } else {
            return CompletableFuture.completedFuture(updatedFlags);
        }
    }

    private Optional<UpdatedFlags> updateFlagsOnMessage(Mailbox<CassandraId> mailbox, FlagsUpdate flagUpdate, MailboxMessage<CassandraId> message) {
        return tryMessageFlagsUpdate(flagUpdate, mailbox, message)
            .map(Optional::of)
            .orElse(handleRetries(mailbox, flagUpdate, message.getUid()));
    }

    private Optional<UpdatedFlags> tryMessageFlagsUpdate(FlagsUpdate flagUpdate, Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> message) {
        try {
            long oldModSeq = message.getModSeq();
            Flags oldFlags = message.createFlags();
            Flags newFlags = flagUpdate.apply(oldFlags);
            message.setFlags(newFlags);
            message.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
            if (messageRepository.conditionalSave(message, oldModSeq)) {
                return Optional.of(new UpdatedFlags(message.getUid(), message.getModSeq(), oldFlags, newFlags));
            } else {
                return Optional.empty();
            }
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    private Optional<UpdatedFlags> handleRetries(Mailbox<CassandraId> mailbox, FlagsUpdate flagUpdate, long uid) {
        try {
            return Optional.of(
                new FunctionRunnerWithRetry(maxRetries)
                    .executeAndRetrieveObject(() -> retryMessageFlagsUpdate(mailbox, uid, flagUpdate)));
        } catch (MessageDeletedDuringFlagsUpdateException e) {
            mailboxSession.getLog().warn(e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private Optional<UpdatedFlags> retryMessageFlagsUpdate(Mailbox<CassandraId> mailbox, long uid, FlagsUpdate flagUpdate) {
        MailboxMessage<CassandraId> reloadedMessage =
            messageRepository.loadMessageRange(mailbox, MessageRange.one(uid))
                .findAny()
                .orElseThrow(() -> new MessageDeletedDuringFlagsUpdateException(mailbox.getMailboxId(), uid));
        return tryMessageFlagsUpdate(flagUpdate, mailbox, reloadedMessage);
    }

}
