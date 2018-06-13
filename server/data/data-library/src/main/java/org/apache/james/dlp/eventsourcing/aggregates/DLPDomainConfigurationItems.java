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

package org.apache.james.dlp.eventsourcing.aggregates;

import java.util.List;
import java.util.stream.Stream;

import org.apache.james.dlp.api.DLPConfigurationItem;
import org.apache.james.dlp.eventsourcing.events.ConfigurationItemsAdded;
import org.apache.james.dlp.eventsourcing.events.ConfigurationItemsRemoved;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.eventstore.History;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class DLPDomainConfigurationItems {

    public static DLPDomainConfigurationItems load(DLPAggregateId aggregateId, History history) {
        return new DLPDomainConfigurationItems(aggregateId, history);
    }

    private static class State {

        static State initial() {
            return new State(ImmutableSet.of());
        }

        final ImmutableSet<DLPConfigurationItem> rules;

        private State(ImmutableSet<DLPConfigurationItem> rules) {
            this.rules = rules;
        }

        State add(DLPConfigurationItem rule) {
            return add(ImmutableList.of(rule));
        }

        State add(List<DLPConfigurationItem> toAdd) {
            ImmutableSet<DLPConfigurationItem> union = Stream.concat(this.rules.stream(), toAdd.stream()).collect(Guavate.toImmutableSet());
            return new State(union);
        }

        State remove(DLPConfigurationItem toRemove) {
            return remove(ImmutableList.of(toRemove));
        }

        State remove(List<DLPConfigurationItem> toRemove) {
            ImmutableSet<DLPConfigurationItem> filtered = rules.stream().filter(rule -> !toRemove.contains(rule)).collect(Guavate.toImmutableSet());
            return new State(filtered);
        }
    }

    private final DLPAggregateId aggregateId;
    private final History history;
    private State state;

    private DLPDomainConfigurationItems(DLPAggregateId aggregateId, History history) {
        this.aggregateId = aggregateId;
        this.state = State.initial();
        history.getEvents().forEach(this::apply);
        this.history = history;
    }

    public Stream<DLPConfigurationItem> retrieveRules() {
        return state.rules.stream();
    }

    public List<Event> clear() {
        ImmutableList<DLPConfigurationItem> rules = retrieveRules().collect(Guavate.toImmutableList());
        if (!rules.isEmpty()) {
            ImmutableList<Event> events = ImmutableList.of(new ConfigurationItemsRemoved(aggregateId, history.getNextEventId(), rules));
            events.forEach(this::apply);
            return events;
        } else {
            return ImmutableList.of();
        }
    }

    public List<Event> store(List<DLPConfigurationItem> rules) {
        ImmutableSet<DLPConfigurationItem> existingRules = retrieveRules().collect(Guavate.toImmutableSet());
        ImmutableList<DLPConfigurationItem> addedRules = rules.stream()
            .filter(rule -> !existingRules.contains(rule))
            .distinct()
            .collect(Guavate.toImmutableList());
        if (!addedRules.isEmpty()) {
            ImmutableList<Event> events = ImmutableList.of(new ConfigurationItemsAdded(aggregateId, history.getNextEventId(), addedRules));
            events.forEach(this::apply);
            return events;
        } else {
            return ImmutableList.of();
        }
    }

    private void apply(Event event) {
        if (event instanceof ConfigurationItemsAdded) {
            state = state.add(((ConfigurationItemsAdded) event).getRules());
        }
        if (event instanceof ConfigurationItemsRemoved) {
            state = state.remove(((ConfigurationItemsRemoved) event).getRules());
        }
    }

}
