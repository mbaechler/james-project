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

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.core.MailAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.Matcher;
import org.apache.mailet.MatcherConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

public class Dlp implements Matcher {

    public static final String DLP_MATCHED_RULE = "DlpMatchedRule";
    private final DlpRulesLoader rulesLoader;

    @Inject
    @VisibleForTesting
    Dlp(DlpRulesLoader rulesLoader) {
        this.rulesLoader = rulesLoader;
    }

    @Override
    public void init(MatcherConfig config) throws MessagingException {
    }

    @Override
    public Collection<MailAddress> match(Mail mail) throws MessagingException {
        Optional<Pair<Mail, DlpDomainRules.Rule>> mailRulePair = findFirstMatchingRule(mail);
        mailRulePair.ifPresent(this::setRuleIdAsMailAttribute);
        return mailRulePair.map(pair -> pair.getLeft().getRecipients()).orElse(ImmutableList.of());
    }

    private void setRuleIdAsMailAttribute(Pair<Mail, DlpDomainRules.Rule> pair) {
        pair.getLeft().setAttribute(DLP_MATCHED_RULE, pair.getRight().id().asString());
    }

    private Optional<Pair<Mail, DlpDomainRules.Rule>> findFirstMatchingRule(Mail mail) {
        return Optional
                .ofNullable(mail.getSender())
                .flatMap(sender -> matchingRule(sender, mail)
                    .map(rule -> Pair.of(mail, rule)));
    }

    private Optional<DlpDomainRules.Rule> matchingRule(MailAddress address, Mail mail) {
        return rulesLoader.load(address.getDomain()).match(mail);
    }

    @Override
    public void destroy() {
    }

    @Override
    public MatcherConfig getMatcherConfig() {
        return null;
    }

    @Override
    public String getMatcherInfo() {
        return "Data Leak Prevention Matcher";
    }
}
