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

import org.apache.james.dlp.api.DLPRule;
import org.apache.james.dlp.eventsourcing.events.RulesAdded;
import org.apache.james.dlp.eventsourcing.events.RulesRemoved;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.eventstore.History;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class DLPDomainRules {

    public static DLPDomainRules load(DLPRulesAggregateId aggregateId, History history) {
        return new DLPDomainRules(aggregateId, history);
    }

    private static class State {

        static State initial() {
            return new State(ImmutableSet.of());
        }

        final ImmutableSet<DLPRule> rules;

        private State(ImmutableSet<DLPRule> rules) {
            this.rules = rules;
        }

        State add(DLPRule rule) {
            return add(ImmutableList.of(rule));
        }

        State add(List<DLPRule> toAdd) {
            ImmutableSet<DLPRule> union = Stream.concat(this.rules.stream(), toAdd.stream()).collect(Guavate.toImmutableSet());
            return new State(union);
        }

        State remove(DLPRule toRemove) {
            return remove(ImmutableList.of(toRemove));
        }

        State remove(List<DLPRule> toRemove) {
            ImmutableSet<DLPRule> filtered = rules.stream().filter(rule -> !toRemove.contains(rule)).collect(Guavate.toImmutableSet());
            return new State(filtered);
        }
    }

    private final DLPRulesAggregateId aggregateId;
    private final History history;
    private State state;

    private DLPDomainRules(DLPRulesAggregateId aggregateId, History history) {
        this.aggregateId = aggregateId;
        this.state = State.initial();
        history.getEvents().forEach(this::apply);
        this.history = history;
    }

    public Stream<DLPRule> retrieveRules() {
        return state.rules.stream();
    }

    public List<Event> clear() {
        ImmutableList<DLPRule> rules = retrieveRules().collect(Guavate.toImmutableList());
        if (!rules.isEmpty()) {
            ImmutableList<Event> events = ImmutableList.of(new RulesRemoved(aggregateId, history.getNextEventId(), rules));
            events.forEach(this::apply);
            return events;
        } else {
            return ImmutableList.of();
        }
    }

    public List<Event> store(List<DLPRule> rules) {
        ImmutableSet<DLPRule> existingRules = retrieveRules().collect(Guavate.toImmutableSet());
        ImmutableList<DLPRule> addedRules = rules.stream()
            .filter(rule -> !existingRules.contains(rule))
            .distinct()
            .collect(Guavate.toImmutableList());
        if (!addedRules.isEmpty()) {
            ImmutableList<Event> events = ImmutableList.of(new RulesAdded(aggregateId, history.getNextEventId(), addedRules));
            events.forEach(this::apply);
            return events;
        } else {
            return ImmutableList.of();
        }
    }

    private void apply(Event event) {
        if (event instanceof RulesAdded) {
            state = state.add(((RulesAdded) event).getRules());
        }
        if (event instanceof RulesRemoved) {
            state = state.remove(((RulesRemoved) event).getRules());
        }
    }

}
