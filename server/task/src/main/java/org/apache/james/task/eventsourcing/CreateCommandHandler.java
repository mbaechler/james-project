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

package org.apache.james.task.eventsourcing;

import java.util.List;

import org.apache.james.eventsourcing.CommandHandler;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.eventstore.EventStore;
import org.apache.james.eventsourcing.eventstore.History;

public class CreateCommandHandler implements CommandHandler<EventSourcingTaskManager.Create> {
    private final EventStore eventStore;

    public CreateCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public Class<EventSourcingTaskManager.Create> handledClass() {
        return EventSourcingTaskManager.Create.class;
    }

    @Override
    public List<? extends Event> handle(EventSourcingTaskManager.Create create) {
        TaskAggregateId aggregateId = new TaskAggregateId(create.getId());
        History history = eventStore.getEventsOfAggregate(aggregateId);
        TaskAggregate aggregate = TaskAggregate.fromHistory(aggregateId, history);
        return aggregate.create(create.getTask());
    }
}
