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
import java.util.Set;

import javax.inject.Inject;

import org.apache.james.eventsourcing.Command;
import org.apache.james.eventsourcing.CommandHandler;
import org.apache.james.eventsourcing.EventSourcingSystem;
import org.apache.james.eventsourcing.Subscriber;
import org.apache.james.eventsourcing.eventstore.EventStore;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskId;
import org.apache.james.task.TaskManager;
import org.apache.james.task.TaskNotFoundException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class EventSourcingTaskManager implements TaskManager {

    private final EventSourcingSystem eventSourcingSystem;
    private final TaskExecutionDetailsProjection executionDetailsProjection;

    @Inject
    @VisibleForTesting
    EventSourcingTaskManager(EventStore eventStore) {
        executionDetailsProjection = new TaskExecutionDetailsProjection();
        Set<CommandHandler<?>> handlers = ImmutableSet.of(new CreateCommandHandler(eventStore));
        Set<Subscriber> subscribers = ImmutableSet.of(event -> {
            if (event instanceof DetailsChanged) {
                DetailsChanged detailsChanged = (DetailsChanged) event;
                executionDetailsProjection.update(detailsChanged.getAggregateId(),
                    detailsChanged.getDetails());
            }
        });
        eventSourcingSystem = new EventSourcingSystem(handlers, subscribers, eventStore);
    }

    public void stop() {
    }

    static class Create implements Command {
        private final TaskId id;
        private final Task task;

        Create(TaskId id, Task task) {
            this.id = id;
            this.task = task;
        }

        public TaskId getId() {
            return id;
        }

        public Task getTask() {
            return task;
        }
    }

    @Override
    public TaskId submit(Task task) {
        TaskId taskId = TaskId.generateTaskId();
        Create command = new Create(taskId, task);
        eventSourcingSystem.dispatch(command);
        return taskId;
    }

    @Override
    public TaskExecutionDetails getExecutionDetails(TaskId id) {
        return executionDetailsProjection.load(id).orElseThrow(TaskNotFoundException::new);
    }

    @Override
    public List<TaskExecutionDetails> list() {
        return ImmutableList.of();
    }

    @Override
    public List<TaskExecutionDetails> list(Status status) {
        return ImmutableList.of();
    }

    @Override
    public void cancel(TaskId id) {

    }

    @Override
    public TaskExecutionDetails await(TaskId id) {
        return null;
    }
}
