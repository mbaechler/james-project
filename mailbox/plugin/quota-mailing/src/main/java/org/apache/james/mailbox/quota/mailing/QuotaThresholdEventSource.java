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

package org.apache.james.mailbox.quota.mailing;

import java.util.List;

import javax.inject.Inject;

import org.apache.james.eventsourcing.CommandDispatcher;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventBus;
import org.apache.james.eventsourcing.EventStore;
import org.apache.james.mailbox.quota.mailing.aggregates.UserQuotaThresholds;
import org.apache.james.mailbox.quota.mailing.commands.DetectThresholdCrossing;
import org.apache.james.mailbox.quota.mailing.subscribers.QuotaThresholdMailer;

public class QuotaThresholdEventSource {

    private final CommandDispatcher commandDispatcher;
    private final EventStore eventStore;
    private final QuotaMailingListenerConfiguration quotaMailingListenerConfiguration;
    private final EventBus eventBus;

    @Inject
    public QuotaThresholdEventSource(EventStore eventStore, EventBus eventBus, QuotaThresholdMailer quotaThresholdMailer, QuotaMailingListenerConfiguration quotaMailingListenerConfiguration) {
        this.eventStore = eventStore;
        this.quotaMailingListenerConfiguration = quotaMailingListenerConfiguration;
        this.eventBus = eventBus.subscribe(quotaThresholdMailer);
        this.commandDispatcher = new CommandDispatcher(this.eventBus)
            .register(DetectThresholdCrossing.class, this::detectThresholdCrossing);
    }

    private List<? extends Event> detectThresholdCrossing(DetectThresholdCrossing command) {
        UserQuotaThresholds.Id aggregateId = UserQuotaThresholds.Id.from(command.getUser());
        List<? extends Event> eventsOfAggregate = eventStore.getEventsOfAggregate(aggregateId).getEvents();
        UserQuotaThresholds aggregate = UserQuotaThresholds.fromEvents(aggregateId, eventsOfAggregate);
        return aggregate.detectThresholdCrossing(quotaMailingListenerConfiguration.getThresholds(), aggregateId.getUser(),
            quotaMailingListenerConfiguration.getGracePeriod(), command.getCountQuota(), command.getSizeQuota(), command.getInstant());
    }

    public CommandDispatcher getCommandDispatcher() {
        return commandDispatcher;
    }
}
