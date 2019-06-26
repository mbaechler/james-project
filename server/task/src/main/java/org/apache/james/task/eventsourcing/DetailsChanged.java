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

import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.task.TaskExecutionDetails;

public class DetailsChanged implements Event {

    private final TaskAggregateId aggregateId;
    private final EventId eventId;
    private final TaskExecutionDetails details;

    public DetailsChanged(TaskAggregateId aggregateId, EventId eventId, TaskExecutionDetails details) {
        this.aggregateId = aggregateId;
        this.eventId = eventId;
        this.details = details;
    }

    @Override
    public EventId eventId() {
        return eventId;
    }

    @Override
    public TaskAggregateId getAggregateId() {
        return aggregateId;
    }

    public TaskExecutionDetails getDetails() {
      return details;
    }
}
