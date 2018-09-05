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

package org.apache.james.queue.rabbitmq;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.blob.api.BlobId;
import org.apache.james.core.MailAddress;
import org.apache.james.util.SerializationUtil;
import org.apache.james.util.streams.Iterators;
import org.apache.mailet.Mail;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class MailDTO {

    static MailDTO fromMail(Mail mail, BlobId headerBlobId, BlobId bodyBlobId) {
        return new MailDTO(
            mail.getRecipients().stream()
                .map(MailAddress::asString)
                .collect(Guavate.toImmutableList()),
            mail.getName(),
            mail.getSender().asString(),
            mail.getState(),
            mail.getErrorMessage(),
            mail.getLastUpdated().toInstant(),
            serializedAttributes(mail),
            mail.getRemoteAddr(),
            mail.getRemoteHost(),
            SerializationUtil.serialize(mail.getPerRecipientSpecificHeaders()),
            headerBlobId.asString(),
            bodyBlobId.asString());
    }

    private static ImmutableMap<String, String> serializedAttributes(Mail mail) {
        return Iterators.toStream(mail.getAttributeNames())
            .map(name -> Pair.of(name, SerializationUtil.serialize(mail.getAttribute(name))))
            .collect(Guavate.toImmutableMap(
                Pair::getKey,
                Pair::getValue));
    }

    private final ImmutableList<String> recipients;
    private final String name;
    private final String sender;
    private final String state;
    private final String errorMessage;
    private final Instant lastUpdated;
    private final ImmutableMap<String, String> attributes;
    private final String remoteAddr;
    private final String remoteHost;
    private final String perRecipientHeaders;
    private final String headerBlobId;
    private final String bodyBlobId;

    @JsonCreator
    private MailDTO(@JsonProperty("recipients") ImmutableList<String> recipients,
                    @JsonProperty("name") String name,
                    @JsonProperty("sender") String sender,
                    @JsonProperty("state") String state,
                    @JsonProperty("errorMessage") String errorMessage,
                    @JsonProperty("lastUpdated") Instant lastUpdated,
                    @JsonProperty("attributes") ImmutableMap<String, String> attributes,
                    @JsonProperty("remoteAddr") String remoteAddr,
                    @JsonProperty("remoteHost") String remoteHost,
                    @JsonProperty("perRecipientHeaders") String perRecipientHeaders,
                    @JsonProperty("headerBlobId") String headerBlobId,
                    @JsonProperty("bodyBlobId") String bodyBlobId) {
        this.recipients = recipients;
        this.name = name;
        this.sender = sender;
        this.state = state;
        this.errorMessage = errorMessage;
        this.lastUpdated = lastUpdated;
        this.attributes = attributes;
        this.remoteAddr = remoteAddr;
        this.remoteHost = remoteHost;
        this.perRecipientHeaders = perRecipientHeaders;
        this.headerBlobId = headerBlobId;
        this.bodyBlobId = bodyBlobId;
    }

    Collection<String> getRecipients() {
        return recipients;
    }

    String getName() {
        return name;
    }

    String getSender() {
        return sender;
    }

    String getState() {
        return state;
    }

    String getErrorMessage() {
        return errorMessage;
    }

    Instant getLastUpdated() {
        return lastUpdated;
    }

    Map<String, String> getAttributes() {
        return attributes;
    }

    String getRemoteAddr() {
        return remoteAddr;
    }

    String getRemoteHost() {
        return remoteHost;
    }

    String getPerRecipientHeaders() {
        return perRecipientHeaders;
    }

    String getHeaderBlobId() {
        return headerBlobId;
    }

    String getBodyBlobId() {
        return bodyBlobId;
    }
}
