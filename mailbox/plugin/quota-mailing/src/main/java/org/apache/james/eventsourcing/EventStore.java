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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

public interface EventStore {

    class History {

        public static History empty() {
            return new History(Optional.empty(), ImmutableList.of());
        }

        public static History of(EventId version, List<? extends Event> events) {
            return new History(Optional.of(version), events);
        }

        private final Optional<EventId> version;
        private final List<? extends Event> events;

        private History(Optional<EventId> version, List<? extends Event> events) {
            this.version = version;
            this.events = events;
        }

        public Optional<EventId> getVersion() {
            return version;
        }

        public List<? extends Event> getEvents() {
            return events;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            History history = (History) o;
            return Objects.equals(version, history.version) &&
                Objects.equals(events, history.events);
        }

        @Override
        public int hashCode() {

            return Objects.hash(version, events);
        }
    }

    default void append(Event event) {
        appendAll(Arrays.asList(event));
    }

    default void appendAll(Event... event) {
        appendAll(Arrays.asList(event));
    }

    /**
     * This method should check that no input event has an id already stored and throw otherwise
     * It should also check that all events belong to the same aggregate
     */
    void appendAll(List<? extends Event> events);

    History getEventsOfAggregate(AggregateId aggregateId);
}
