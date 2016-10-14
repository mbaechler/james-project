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

package org.apache.james.jmap.methods;

import java.util.function.Function;

import javax.inject.Inject;
import javax.mail.Flags;

import org.apache.james.jmap.exceptions.MessageNotFoundException;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMessagesRequest;
import org.apache.james.jmap.model.SetMessagesResponse;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class SetMessagesDestructionProcessor implements SetMessagesProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetMessagesCreationProcessor.class);

    private final MessageIdManager messageIdManager;

    @Inject
    @VisibleForTesting
    SetMessagesDestructionProcessor(MessageIdManager messageIdManager) {
        this.messageIdManager = messageIdManager;
    }

    @Override
    public SetMessagesResponse process(SetMessagesRequest request, MailboxSession mailboxSession) {
        return request.getDestroy().stream()
                .map(delete(mailboxSession))
                .reduce(SetMessagesResponse.builder(),  SetMessagesResponse.Builder::accumulator, SetMessagesResponse.Builder::combiner)
                .build();
    }

    private Function<? super MessageId, SetMessagesResponse> delete(MailboxSession mailboxSession) {
        return (messageId) -> {
            try {
                checkThatMessageExists(messageId, mailboxSession);
                removeMessage(messageId, mailboxSession);
                return SetMessagesResponse.builder().destroyed(messageId).build();
            } catch (MessageNotFoundException e) {
                return SetMessagesResponse.builder().notDestroyed(messageId,
                        SetError.builder()
                                .type("notFound")
                                .description("The message " + messageId.serialize() + " can't be found")
                                .build())
                        .build();
            } catch (MailboxException e) {
                LOGGER.error("An error occurred when deleting a message", e);
                return SetMessagesResponse.builder().notDestroyed(messageId,
                        SetError.builder()
                                .type("anErrorOccurred")
                                .description("An error occurred while deleting message " + messageId.serialize())
                                .build())
                        .build();
            }
        };
    }

    private void checkThatMessageExists(MessageId messageId, MailboxSession mailboxSession) throws MailboxException, MessageNotFoundException {
        Optional<MessageResult> messages = messageIdManager.getMessages(messageId, FetchGroupImpl.MINIMAL, mailboxSession);
        if (!messages.isPresent()) {
            throw new MessageNotFoundException();
        }
    }

    private void removeMessage(MessageId messageId, MailboxSession mailboxSession) throws MailboxException {
        messageIdManager.setFlags(new Flags(Flags.Flag.DELETED), MessageManager.FlagsUpdateMode.ADD, messageId, mailboxSession);
        messageIdManager.expunge(messageId, mailboxSession);
    }
}
