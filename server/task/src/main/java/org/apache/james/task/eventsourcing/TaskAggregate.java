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

import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.eventstore.History;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;

import com.google.common.collect.ImmutableList;

public class TaskAggregate {
    public static TaskAggregate fromHistory(TaskAggregateId aggregateId, History history) {
        return new TaskAggregate(aggregateId, history);
    }

    private final TaskAggregateId aggregateId;
    private final History history;

    private TaskAggregate(TaskAggregateId aggregateId, History history) {
        this.aggregateId = aggregateId;
        this.history = history;
    }

    public List<Event> create(Task task) {
        TaskExecutionDetails completed = TaskExecutionDetails
            .from(task, aggregateId.getTaskId())
            .start()
            .completed();
        return ImmutableList.of(new DetailsChanged(aggregateId, history.getNextEventId(), completed));
    }
}
