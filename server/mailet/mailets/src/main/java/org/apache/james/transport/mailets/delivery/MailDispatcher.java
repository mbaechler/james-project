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
package org.apache.james.transport.mailets.delivery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.server.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetContext;
import org.apache.mailet.PerRecipientHeaders.Header;
import org.apache.mailet.base.RFC2822Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.runnable.ThrowingRunnable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class MailDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailDispatcher.class);
    private static final String[] NO_HEADERS = {};
    private static final int RETRIES = 3;
    private static final Duration FIRST_BACKOFF = Duration.ofMillis(200);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(1);

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        static final boolean CONSUME = true;
        private MailStore mailStore;
        private Optional<Boolean> consume = Optional.empty();
        private MailetContext mailetContext;

        public Builder consume(boolean consume) {
            this.consume = Optional.of(consume);
            return this;
        }

        public Builder mailStore(MailStore mailStore) {
            this.mailStore = mailStore;
            return this;
        }

        public Builder mailetContext(MailetContext mailetContext) {
            this.mailetContext = mailetContext;
            return this;
        }

        public MailDispatcher build() {
            Preconditions.checkNotNull(mailStore);
            Preconditions.checkNotNull(mailetContext);
            return new MailDispatcher(mailStore, consume.orElse(CONSUME), mailetContext);
        }

    }

    private final MailStore mailStore;
    private final boolean consume;
    private final MailetContext mailetContext;
    private final Scheduler scheduler;

    private MailDispatcher(MailStore mailStore, boolean consume, MailetContext mailetContext) {
        this.mailStore = mailStore;
        this.consume = consume;
        this.mailetContext = mailetContext;
        this.scheduler = Schedulers.boundedElastic();
    }

    public void dispatch(Mail mail) throws MessagingException {
        List<MailAddress> errors =  customizeHeadersAndDeliver(mail);
        if (!errors.isEmpty()) {
            // If there were errors, we redirect the email to the ERROR
            // processor.
            // In order for this server to meet the requirements of the SMTP
            // specification, mails on the ERROR processor must be returned to
            // the sender. Note that this email doesn't include any details
            // regarding the details of the failure(s).
            // In the future we may wish to address this.
            Mail newMail = MailImpl.builder()
                .name("error-" + mail.getName())
                .sender(mail.getMaybeSender())
                .addRecipients(errors)
                .mimeMessage(mail.getMessage())
                .state(Mail.ERROR)
                .build();
            mailetContext.sendMail(newMail);
        }
        if (consume) {
            // Consume this message
            mail.setState(Mail.GHOST);
        }
    }

    private List<MailAddress> customizeHeadersAndDeliver(Mail mail) throws MessagingException {
        MimeMessage message = mail.getMessage();
        // Set Return-Path and remove all other Return-Path headers from the message
        // This only works because there is a placeholder inserted by MimeMessageWrapper
        message.setHeader(RFC2822Headers.RETURN_PATH, mail.getMaybeSender().asPrettyString());

        List<MailAddress> errors = deliver(mail, message);

        return errors;
    }

    private List<MailAddress> deliver(Mail mail, MimeMessage message) {
        List<MailAddress> errors = new ArrayList<>();
        for (MailAddress recipient : mail.getRecipients()) {
            try {
                Map<String, List<String>> savedHeaders = saveHeaders(mail, recipient);

                addSpecificHeadersForRecipient(mail, message, recipient);
                storeMailWithRetry(mail, recipient).block();

                restoreHeaders(mail.getMessage(), savedHeaders);
            } catch (Exception ex) {
                LOGGER.error("Error while storing mail.", ex);
                errors.add(recipient);
            }
        }
        return errors;
    }

    private Mono<Void> storeMailWithRetry(Mail mail, MailAddress recipient) {
       return Mono.fromRunnable((ThrowingRunnable)() -> mailStore.storeMail(recipient, mail))
           .doOnError(error -> LOGGER.error("Error While storing mail.", error))
           .subscribeOn(scheduler)
           .retryBackoff(RETRIES, FIRST_BACKOFF, MAX_BACKOFF, scheduler)
           .then();
    }

    private Map<String, List<String>> saveHeaders(Mail mail, MailAddress recipient) throws MessagingException {
        ImmutableMap.Builder<String, List<String>> backup = ImmutableMap.builder();
        Collection<String> headersToSave = mail.getPerRecipientSpecificHeaders().getHeaderNamesForRecipient(recipient);
        for (String headerName: headersToSave) {
            List<String> values = ImmutableList.copyOf(
                        Optional.ofNullable(mail.getMessage().getHeader(headerName))
                            .orElse(NO_HEADERS));
            backup.put(headerName, values);
        }
        return backup.build();
    }

    private void restoreHeaders(MimeMessage mimeMessage, Map<String, List<String>> savedHeaders) throws MessagingException {
        for (Map.Entry<String, List<String>> header: savedHeaders.entrySet()) {
            String name = header.getKey();
            mimeMessage.removeHeader(name);
            for (String value: header.getValue()) {
                mimeMessage.addHeader(name, value);
            }
        }
    }

    private void addSpecificHeadersForRecipient(Mail mail, MimeMessage message, MailAddress recipient) throws MessagingException {
        for (Header header: mail.getPerRecipientSpecificHeaders().getHeadersForRecipient(recipient)) {
            message.addHeader(header.getName(), header.getValue());
        }
    }
}
