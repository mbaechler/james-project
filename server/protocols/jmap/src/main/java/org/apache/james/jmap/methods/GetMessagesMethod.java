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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.apache.james.jmap.model.GetMessagesRequest;
import org.apache.james.jmap.model.GetMessagesResponse;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.MessageId;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.MessageResultIterator;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.javatuples.Pair;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class GetMessagesMethod<Id extends MailboxId> implements Method {

    private static final Method.Name METHOD_NAME = Method.name("getMessages");
    private final MailboxManager mailboxManager;
    

    @Inject
    @VisibleForTesting GetMessagesMethod(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }
    
    @Override
    public Method.Name methodName() {
        return METHOD_NAME;
    }
    
    @Override
    public Class<? extends JmapRequest> requestType() {
        return GetMessagesRequest.class;
    }
    
    @Override
    public GetMessagesResponse process(JmapRequest request, MailboxSession mailboxSession) {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(mailboxSession);
        Preconditions.checkArgument(request instanceof GetMessagesRequest);
        GetMessagesRequest getMessagesRequest = (GetMessagesRequest) request;
        
        Function<MessageId, Stream<Pair<MessageResult, MailboxPath>>> loadMessages = loadMessage(mailboxSession);
        Function<Pair<MessageResult, MailboxPath>, Message> toJmapMessage = toJmapMessage(mailboxSession);
        
        List<Message> result = getMessagesRequest.getMessageIds().stream()
            .flatMap(loadMessages)
            .map(toJmapMessage)
            .collect(Collectors.toList());

        return new GetMessagesResponse(result);
    }

    private Function<Pair<MessageResult, MailboxPath>, Message> toJmapMessage(MailboxSession mailboxSession) {
        return (value) -> {
            long mailUid = value.getValue0().getUid();
            MailboxPath mailboxPath = value.getValue1();
            return new Message(
                        new MessageId(mailboxSession.getUser(), mailboxPath , mailUid));
        };
    }

    private Function<MessageId, Stream<Pair<MessageResult, MailboxPath>>> loadMessage(MailboxSession mailboxSession) {
        return Throwing
                .function((MessageId messageId) -> {
                     MailboxPath mailboxPath = messageId.getMailboxPath(mailboxSession);
                     MessageManager mailbox = mailboxManager.getMailbox(mailboxPath, mailboxSession);
                     return Pair.with(
                             mailbox.getMessages(MessageRange.one(messageId.getUid()), FetchGroupImpl.MINIMAL, mailboxSession),
                             mailboxPath
                             );
         })
                .andThen(this::iteratorToStream);
    }
    
    private Stream<Pair<MessageResult, MailboxPath>> iteratorToStream(Pair<MessageResultIterator, MailboxPath> value) {
        Iterable<MessageResult> iterable = () -> value.getValue0();
        Stream<MessageResult> targetStream = StreamSupport.stream(iterable.spliterator(), false);
        
        MailboxPath mailboxPath = value.getValue1();
        return targetStream.map(x -> Pair.with(x, mailboxPath));
    }

}
