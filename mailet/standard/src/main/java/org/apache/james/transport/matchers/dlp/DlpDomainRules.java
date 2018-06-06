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

package org.apache.james.transport.matchers.dlp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.dlp.api.DLPConfigurationItem;
import org.apache.james.util.OptionalUtils;
import org.apache.mailet.Mail;

import com.github.fge.lambdas.predicates.ThrowingPredicate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

public class DlpDomainRules {

    @VisibleForTesting static DlpDomainRules matchNothing() {
        return DlpDomainRules.of(new Rule(DLPConfigurationItem.Id.of("always false"), (mail) -> false));
    }

    @VisibleForTesting static DlpDomainRules matchAll() {
        return DlpDomainRules.of(new Rule(DLPConfigurationItem.Id.of("always true"), (mail) -> true));
    }

    private static DlpDomainRules of(Rule rule) {
        return new DlpDomainRules(ImmutableList.of(rule));
    }

    public static DlpDomainRulesBuilder builder() {
        return new DlpDomainRulesBuilder();
    }

    static class Rule {

        interface MatcherFunction extends ThrowingPredicate<Mail> { }

        private final DLPConfigurationItem.Id id;
        private final MatcherFunction matcher;

        public Rule(DLPConfigurationItem.Id id, MatcherFunction matcher) {
            this.id = id;
            this.matcher = matcher;
        }

        public DLPConfigurationItem.Id id() {
            return id;
        }

        public boolean match(Mail mail) {
            return matcher.test(mail);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Rule) {
                Rule other = (Rule) o;
                return Objects.equals(id, other.id) &&
                    Objects.equals(matcher, other.matcher);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, matcher);
        }

    }

    public static class DlpDomainRulesBuilder {

        private final ImmutableList.Builder<Rule> rules;

        private DlpDomainRulesBuilder() {
            rules = ImmutableList.builder();
        }

        public DlpDomainRulesBuilder recipientRule(DLPConfigurationItem.Id id, Pattern pattern) {
            rules.add(
                new Rule(
                    id,
                    (Mail mail) -> listRecipientsAsString(mail).anyMatch(pattern.asPredicate())));
            return this;
        }

        private Stream<String> listRecipientsAsString(Mail mail) throws MessagingException {
            return Stream.concat(listEnvelopRecipients(mail), listHeaderRecipients(mail));
        }

        private Stream<String> listEnvelopRecipients(Mail mail) {
            return mail.getRecipients().stream().map(MailAddress::asString);
        }

        private Stream<String> listHeaderRecipients(Mail mail) throws MessagingException {
            MimeMessage message = mail.getMessage();
            if (message != null) {
                return Arrays.stream(message.getAllRecipients()).map(Address::toString);
            }
            return Stream.of();
        }

        public DlpDomainRulesBuilder senderRule(DLPConfigurationItem.Id id, Pattern pattern) {
            rules.add(
                new Rule(
                    id,
                    (Mail mail) -> listSenders(mail).anyMatch(pattern.asPredicate())));
            return this;
        }

        private Stream<String> listSenders(Mail mail) throws MessagingException {
            return Stream.concat(listEnvelopSender(mail), listFromHeaders(mail));
        }

        private Stream<String> listEnvelopSender(Mail mail) {
            return OptionalUtils.toStream(Optional.ofNullable(mail.getSender()).map(MailAddress::asString));
        }

        private Stream<String> listFromHeaders(Mail mail) throws MessagingException {
            MimeMessage message = mail.getMessage();
            if (message != null) {
                return Arrays.stream(message.getFrom()).map(Address::toString);
            }
            return Stream.of();
        }

        public DlpDomainRulesBuilder contentRule(DLPConfigurationItem.Id id, Pattern pattern) {
            rules.add(
                new Rule(
                    id,
                    (Mail mail) -> matchSubject(pattern, mail) || matchBody(pattern, mail)));
            return this;
        }

        private boolean matchSubject(Pattern pattern, Mail mail) throws MessagingException {
            return getMessageSubjects(mail).anyMatch(pattern.asPredicate());
        }

        private Stream<String> getMessageSubjects(Mail mail) throws MessagingException {
            MimeMessage message = mail.getMessage();
            if (message != null) {
                return Stream.of(message.getSubject());
            }
            return Stream.of();
        }

        private boolean matchBody(Pattern pattern, Mail mail) throws MessagingException, IOException {
            return getMessageBodies(mail).anyMatch(pattern.asPredicate());
        }

        private Stream<String> getMessageBodies(Mail mail) throws MessagingException, IOException {
            MimeMessage message = mail.getMessage();
            if (message != null) {
                Object content = message.getContent();
                if (content instanceof String) {
                    return Stream.of((String)content);
                }
            }
            return Stream.of();
        }

        public DlpDomainRules build() {
            return new DlpDomainRules(rules.build());
        }
    }

    private final ImmutableList<Rule> rules;

    private DlpDomainRules(ImmutableList<Rule> rules) {
        this.rules = rules;
    }

    public Optional<Rule> match(Mail mail) {
        return rules.stream().flatMap(rule -> Stream.of(rule).filter(x -> x.match(mail))).findFirst();
    }

}
