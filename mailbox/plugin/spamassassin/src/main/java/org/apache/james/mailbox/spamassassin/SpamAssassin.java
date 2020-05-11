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
package org.apache.james.mailbox.spamassassin;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.spamassassin.MessageToLearn;
import org.apache.james.spamassassin.SpamAssassinInvoker;
import org.apache.james.util.Host;

import reactor.core.publisher.Mono;

public class SpamAssassin {

    private final Optional<SpamAssassinInvoker> invoker;

    @Inject
    public SpamAssassin(MetricFactory metricFactory, SpamAssassinConfiguration spamAssassinConfiguration) {
        if (spamAssassinConfiguration.isEnable()) {
            Host host = spamAssassinConfiguration.getHost().get();
            invoker = Optional.of(new SpamAssassinInvoker(metricFactory, host.getHostName(), host.getPort()));
        } else {
            invoker = Optional.empty();
        }
    }

    public Mono<Void> learnSpam(MessageToLearn message, Username username) {
        return invoker.map(invoker ->
            Mono.from(invoker.learnAsSpam(message, username))
            .then())
            .orElse(Mono.empty());
    }

    public Mono<Void> learnHam(MessageToLearn message, Username username) {
        return invoker.map(invoker ->
            Mono.from(invoker.learnAsHam(message, username))
                .then())
            .orElse(Mono.empty());
    }
}
