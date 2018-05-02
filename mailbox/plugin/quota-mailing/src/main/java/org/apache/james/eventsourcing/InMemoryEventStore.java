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

package org.apache.james.eventsourcing;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class InMemoryEventStore implements EventStore {

    private static class InMemoryHistory {
        static InMemoryHistory of(History history) {
            return new InMemoryHistory(history);
        }

        final History history;
        private final Optional<EventId> version;

        InMemoryHistory(History history) {
            this.history = history;
            this.version = history.getVersion();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InMemoryHistory that = (InMemoryHistory) o;
            return Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version);
        }
    }

    private final ConcurrentHashMap<AggregateId, InMemoryHistory> store;

    public InMemoryEventStore() {
        this.store = new ConcurrentHashMap<>();
    }

    @Override
    public void appendAll(List<? extends Event> events) {
        if (events.isEmpty()) {
            return;
        }
        Preconditions.checkArgument(belongsToSameAggregate(events));
        AggregateId aggregateId = events.stream().map(Event::getAggregateId).findFirst().get();
        Optional<EventId> expectedHistoryVersion = events.stream().map(Event::eventId).min(Comparator.naturalOrder()).get().previous();
        EventId updatedHistoryVersion = events.stream().map(Event::eventId).max(Comparator.naturalOrder()).get();

        if (!expectedHistoryVersion.isPresent()) {
            InMemoryHistory inMemoryHistory = store.putIfAbsent(aggregateId, InMemoryHistory.of(History.of(updatedHistoryVersion, events)));
            if (inMemoryHistory != null) {
                throw new EventBus.EventStoreFailedException();
            }
            return;
        }

        InMemoryHistory expectedHistory = InMemoryHistory.of(History.of(expectedHistoryVersion.get(), ImmutableList.of()));
        List<Event> updatedEvents = updatedEvents(events, aggregateId);
        InMemoryHistory updatedHistory = InMemoryHistory.of(History.of(updatedHistoryVersion, updatedEvents));

        if (!store.replace(aggregateId, expectedHistory, updatedHistory)) {
            throw new EventBus.EventStoreFailedException();
        }
    }

    private boolean belongsToSameAggregate(List<? extends Event> events) {
        return events.stream().map(Event::getAggregateId).distinct().limit(2).count() <= 1;
    }

    private List<Event> updatedEvents(List<? extends Event> newEvents, AggregateId aggregateId) {
        History currentHistory = store.get(aggregateId).history;
        return Stream.concat(
            currentHistory.getEvents().stream(),
            newEvents.stream())
            .collect(Guavate.toImmutableList());
    }

    @Override
    public History getEventsOfAggregate(AggregateId aggregateId) {
        return store.getOrDefault(aggregateId, InMemoryHistory.of(History.empty())).history;
    }
}
