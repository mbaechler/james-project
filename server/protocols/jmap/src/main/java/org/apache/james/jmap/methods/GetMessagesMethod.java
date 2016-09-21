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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.json.FieldNamePropertyFilter;
import org.apache.james.jmap.model.ClientId;
import org.apache.james.jmap.model.GetMessagesRequest;
import org.apache.james.jmap.model.GetMessagesResponse;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.MessageFactory;
import org.apache.james.jmap.model.MessageFactory.MetaDataWithContent;
import org.apache.james.jmap.model.MessageProperties;
import org.apache.james.jmap.model.MessageProperties.HeaderProperty;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageResult;

import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.ThrowingFunction;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class GetMessagesMethod implements Method {

    public static final String HEADERS_FILTER = "headersFilter";
    private static final Method.Request.Name METHOD_NAME = Method.Request.name("getMessages");
    private static final Method.Response.Name RESPONSE_NAME = Method.Response.name("messages");
    private final MessageFactory messageFactory;
    private final MessageIdManager messageIdManager;

    @Inject
    @VisibleForTesting GetMessagesMethod(
            MessageFactory messageFactory,
            MessageIdManager messageIdManager) {
        this.messageFactory = messageFactory;
        this.messageIdManager = messageIdManager;
    }
    
    @Override
    public Method.Request.Name requestHandled() {
        return METHOD_NAME;
    }
    
    @Override
    public Class<? extends JmapRequest> requestType() {
        return GetMessagesRequest.class;
    }
    
    @Override
    public Stream<JmapResponse> process(JmapRequest request, ClientId clientId, MailboxSession mailboxSession) {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(mailboxSession);
        Preconditions.checkArgument(request instanceof GetMessagesRequest);
        GetMessagesRequest getMessagesRequest = (GetMessagesRequest) request;
        MessageProperties outputProperties = getMessagesRequest.getProperties().toOutputProperties();
        return Stream.of(JmapResponse.builder().clientId(clientId)
                            .response(getMessagesResponse(mailboxSession, getMessagesRequest))
                            .responseName(RESPONSE_NAME)
                            .properties(outputProperties.getOptionalMessageProperties())
                            .filterProvider(buildOptionalHeadersFilteringFilterProvider(outputProperties))
                            .build());
    }

    private Optional<SimpleFilterProvider> buildOptionalHeadersFilteringFilterProvider(MessageProperties properties) {
        return properties.getOptionalHeadersProperties()
            .map(this::buildHeadersPropertyFilter)
            .map(propertyFilter -> new SimpleFilterProvider()
                .addFilter(HEADERS_FILTER, propertyFilter));
    }
    
    private PropertyFilter buildHeadersPropertyFilter(ImmutableSet<HeaderProperty> headerProperties) {
        return new FieldNamePropertyFilter((fieldName) -> headerProperties.contains(HeaderProperty.fromFieldName(fieldName)));
    }

    private GetMessagesResponse getMessagesResponse(MailboxSession mailboxSession, GetMessagesRequest getMessagesRequest) {
        getMessagesRequest.getAccountId().ifPresent(GetMessagesMethod::notImplemented);
        
        Function<MessageId, Stream<MetaDataWithContent>> loadMessages = loadMessage(mailboxSession);
        Function<MetaDataWithContent, Message> convertToJmapMessage = Throwing.function(messageFactory::fromMetaDataWithContent).sneakyThrow();
        
        List<Message> result = getMessagesRequest.getIds().stream()
            .flatMap(loadMessages)
            .map(convertToJmapMessage)
            .collect(Guavate.toImmutableList());

        return GetMessagesResponse.builder().messages(result).expectedMessageIds(getMessagesRequest.getIds()).build();
    }

    private static void notImplemented(String input) {
        throw new NotImplementedException();
    }

    private Function<MessageId, Stream<MetaDataWithContent>> loadMessage(MailboxSession mailboxSession) {
        ThrowingFunction<MessageId, Stream<MetaDataWithContent>> toMetaDataWithContentStream = (MessageId messageId) -> {
            com.google.common.base.Optional<MessageResult> maybeMessage = messageIdManager.getMessages(messageId, FetchGroupImpl.FULL_CONTENT, mailboxSession);
            
            return maybeMessage.asSet().stream()
                    .map(Throwing.function(MetaDataWithContent::builderFromMessageResult).sneakyThrow())
                    .map(builder -> builder.messageId(messageId))
                    .map(MetaDataWithContent.Builder::build);
        };
        return Throwing.function(toMetaDataWithContentStream).sneakyThrow();
    }
}