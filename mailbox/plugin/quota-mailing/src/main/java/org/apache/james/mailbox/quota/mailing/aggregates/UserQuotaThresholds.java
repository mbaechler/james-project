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

package org.apache.james.mailbox.quota.mailing.aggregates;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.james.core.User;
import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.quota.mailing.events.QuotaThresholdChangedEvent;
import org.apache.james.mailbox.quota.model.HistoryEvolution;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;
import org.apache.james.mailbox.quota.model.QuotaThresholdHistory;
import org.apache.james.mailbox.quota.model.QuotaThresholds;
import org.apache.james.util.OptionalUtils;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

public class UserQuotaThresholds {

    public static class Id implements AggregateId {

        public static Id from(User user) {
            return new Id(user);
        }

        private final User user;

        private Id(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id that = (Id) o;
            return Objects.equals(user, that.user);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user);
        }
    }

    public static UserQuotaThresholds fromEvents(Id aggregateId, List<? extends Event> events) {
        return new UserQuotaThresholds(aggregateId, events);
    }

    private final Id aggregateId;
    private final List<QuotaThresholdChangedEvent> events;

    private UserQuotaThresholds(Id aggregateId, List<? extends Event> events) {
        this.aggregateId = aggregateId;
        this.events = events.stream().map(QuotaThresholdChangedEvent.class::cast).collect(Collectors.toList());
    }

    public List<QuotaThresholdChangedEvent> detectThresholdCrossing(QuotaThresholds configuration, User user,
                                               Duration gracePeriod,
                                               Quota<QuotaCount> countQuota, Quota<QuotaSize> sizeQuota,
                                               Instant instant) {

        List<QuotaThresholdChangedEvent> events = generateEvents(configuration, gracePeriod, countQuota, sizeQuota, instant);
        events.forEach(this::apply);
        return events;
    }

    public List<QuotaThresholdChangedEvent> generateEvents(QuotaThresholds configuration, Duration gracePeriod, Quota<QuotaCount> countQuota, Quota<QuotaSize> sizeQuota, Instant now) {
        QuotaThresholdChange countThresholdChange = new QuotaThresholdChange(configuration.highestExceededThreshold(countQuota), now);
        QuotaThresholdChange sizeThresholdChange = new QuotaThresholdChange(configuration.highestExceededThreshold(sizeQuota), now);

        QuotaThresholdHistory countHistory = new QuotaThresholdHistory(
            events.stream()
                .map(QuotaThresholdChangedEvent::getCountHistoryEvolution)
                .map(HistoryEvolution::getThresholdChange)
                .flatMap(OptionalUtils::toStream)
                .collect(Guavate.toImmutableList()));

        QuotaThresholdHistory sizeHistory = new QuotaThresholdHistory(
            events.stream()
                .map(QuotaThresholdChangedEvent::getSizeHistoryEvolution)
                .map(HistoryEvolution::getThresholdChange)
                .flatMap(OptionalUtils::toStream)
                .collect(Guavate.toImmutableList()));

        HistoryEvolution countHistoryEvolution = countHistory.compareWithCurrentThreshold(countThresholdChange, gracePeriod);
        HistoryEvolution sizeHistoryEvolution = sizeHistory.compareWithCurrentThreshold(sizeThresholdChange, gracePeriod);

        if (countHistoryEvolution.isChange() || sizeHistoryEvolution.isChange()) {
            return ImmutableList.of(
                new QuotaThresholdChangedEvent(
                    nextId(),
                    sizeHistoryEvolution,
                    countHistoryEvolution,
                    sizeQuota,
                    countQuota,
                    aggregateId));
        }

        return ImmutableList.of();
    }

    private EventId nextId() {
        return events.stream().map(Event::eventId).max(Comparator.naturalOrder()).map(EventId::next).orElse(EventId.first());
    }

    private void apply(QuotaThresholdChangedEvent event) {
        events.add(event);
    }


}
