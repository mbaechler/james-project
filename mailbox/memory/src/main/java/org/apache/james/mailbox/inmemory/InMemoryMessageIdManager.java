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

package org.apache.james.mailbox.inmemory;

import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.mail.Flags;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageManager.FlagsUpdateMode;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.MessageResult.FetchGroup;
import org.apache.james.mailbox.model.MessageResultIterator;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class InMemoryMessageIdManager implements MessageIdManager {

    private final MailboxManager mailboxManager;

    @Inject
    public InMemoryMessageIdManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }
    
    private Optional<MessageResult> findMessageWithId(MessageManager messageManager, MessageId messageId, FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxException {
        MessageResultIterator messages = messageManager.getMessages(MessageRange.all(), fetchGroup, mailboxSession);
        while (messages.hasNext()) {
            MessageResult message = messages.next();
            if (message.getMessageId().equals(messageId)) {
                return Optional.of(message);
            }
        }
        return Optional.absent();
    }
    
    @Override
    public void setFlags(Flags newState, FlagsUpdateMode flagsUpdateMode, MessageId messageId, MailboxId mailboxId,
            MailboxSession mailboxSession) throws MailboxException {
        MessageManager messageManager = mailboxManager.getMailbox(mailboxId, mailboxSession);
        Optional<MessageResult> message = findMessageWithId(messageManager, messageId, FetchGroupImpl.MINIMAL, mailboxSession);
        if (!message.isPresent()) {
            throw new MailboxException();
        }
        messageManager.setFlags(newState, flagsUpdateMode, message.get().getUid().toRange(), mailboxSession);
    }

    @Override
    public List<MessageResult> getMessages(List<MessageId> messages, FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxException {
        List<MessageResult> result = Lists.newArrayList();
        for (MailboxPath path: mailboxManager.list(mailboxSession)) {
            MessageManager messageManager = mailboxManager.getMailbox(path, mailboxSession);
            for (MessageId messageId: messages) {
                Optional<MessageResult> maybeMessage = findMessageWithId(messageManager, messageId, fetchGroup, mailboxSession);
                result.addAll(maybeMessage.asSet());
            }
        }
        return result;
    }

    @Override
    public void delete(MessageId messageId, List<MailboxId> mailboxIds, MailboxSession mailboxSession) throws MailboxException {
        for (MailboxId mailboxId: mailboxIds) {
            MessageManager messageManager = mailboxManager.getMailbox(mailboxId, mailboxSession);
            Optional<MessageResult> maybeMessage = findMessageWithId(messageManager, messageId, FetchGroupImpl.MINIMAL, mailboxSession);
            if (maybeMessage.isPresent()) {
                MessageRange range = maybeMessage.get().getUid().toRange();
                messageManager.setFlags(new Flags(Flags.Flag.DELETED), FlagsUpdateMode.REMOVE, range, mailboxSession);
                messageManager.expunge(range, mailboxSession);
            }
        }
    }

    @Override
    public void setInMailboxes(MessageId messageId, List<MailboxId> mailboxIds, MailboxSession mailboxSession) throws MailboxException {
        List<MessageResult> messages = getMessages(ImmutableList.of(messageId), FetchGroupImpl.MINIMAL, mailboxSession);
        ImmutableSet<MailboxId> currentMailboxes = FluentIterable.from(messages).transform(new Function<MessageResult, MailboxId>() {
            @Override
            public MailboxId apply(MessageResult message) {
                return message.getMailboxId();
            }
        }).toSet();
        HashSet<MailboxId> targetMailboxes = Sets.newHashSet(mailboxIds);
        SetView<MailboxId> mailboxesToRemove = Sets.difference(currentMailboxes, targetMailboxes);
        SetView<MailboxId> mailboxesToAdd = Sets.difference(targetMailboxes, currentMailboxes);
        MessageResult referenceMessage = Iterables.getLast(messages);
        for (MailboxId mailboxId: mailboxesToAdd) {
            mailboxManager.copyMessages(referenceMessage.getUid().toRange(), referenceMessage.getMailboxId(), mailboxId, mailboxSession);
        }
        for (MessageResult message: messages) {
            MessageRange range = message.getUid().toRange();
            MailboxId mailboxId = message.getMailboxId();
            if (mailboxesToRemove.contains(mailboxId)) {
                MessageManager messageManager = mailboxManager.getMailbox(mailboxId, mailboxSession);
                messageManager.setFlags(new Flags(Flags.Flag.DELETED), FlagsUpdateMode.ADD, range, mailboxSession);
                messageManager.expunge(range, mailboxSession);
            }
        }
    }

}
