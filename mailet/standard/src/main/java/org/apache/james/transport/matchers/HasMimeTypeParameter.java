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


package org.apache.james.transport.matchers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;


/**
 * <p>This matcher checks if the content type matches.</p>
 *
 * use: <pre>
 *     <code>
 *         <mailet match="HasMimeTypeParameter=report-type=disposition-notification" class="..." />
 *     </code>
 * </pre>
 */
public class HasMimeTypeParameter extends GenericMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HasMimeTypeParameter.class);

    private Map<String, String> filteredMimeTypeParameters;

    @Override
    public void init() throws MessagingException {
        filteredMimeTypeParameters = Splitter.on(",")
            .trimResults()
            .withKeyValueSeparator('=')
            .split(getCondition());
    }

    @Override
    public Collection<MailAddress> match(Mail mail) throws MessagingException {
        Optional<MimeType> maybeMimeType = getMimeTypeFromMessage(mail.getMessage());
        if (maybeMimeType.map(this::mimeTypeMatchParameter).orElse(false)) {
            return mail.getRecipients();
        }
        return ImmutableList.of();
    }

    private Optional<MimeType> getMimeTypeFromMessage(MimeMessage message) throws MessagingException {
        try {
            return Optional.of(new MimeType(message.getContentType()));
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Error while parsing message's mimeType {}", message.getContentType(), e);
            return Optional.empty();
        }
    }

    private boolean mimeTypeMatchParameter(MimeType mimeType) {
        return filteredMimeTypeParameters
            .entrySet()
            .stream()
            .anyMatch(entry -> mimeTypeContainsParameter(mimeType, entry.getKey(), entry.getValue()));
    }

    private boolean mimeTypeContainsParameter(MimeType mimeType, String name, String value) {
        return mimeType.getParameter(name).equalsIgnoreCase(value);
    }

}

