/**
 * *************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ***************************************************************/

package org.apache.james;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.james.task.MemoryWorkQueue;
import org.apache.james.task.SerialTaskManagerWorker;
import org.apache.james.task.TaskManager;
import org.apache.james.task.TaskManagerWorker;
import org.apache.james.task.eventsourcing.EventSourcingTaskManager;
import org.apache.james.task.eventsourcing.Hostname;
import org.apache.james.task.eventsourcing.TaskExecutionDetailsProjection;
import org.apache.james.task.eventsourcing.WorkQueueSupplier;
import org.apache.james.task.eventsourcing.WorkerStatusListener;
import org.apache.james.task.eventsourcing.cassandra.CassandraTaskExecutionDetailsProjection;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class DistributedTaskManagerModule extends AbstractModule {
    private static class UnconfigurableHostname extends RuntimeException {
        UnconfigurableHostname(String message, Exception originException) {
            super(message, originException);
        }
    }

    public static final WorkQueueSupplier workQueueSupplier = eventSourcingSystem -> {
        WorkerStatusListener listener = new WorkerStatusListener(eventSourcingSystem);
        TaskManagerWorker worker = new SerialTaskManagerWorker(listener);
        return new MemoryWorkQueue(worker);
    };

    @Override
    protected void configure() {
        bind(TaskExecutionDetailsProjection.class).in(Scopes.SINGLETON);
        bind(TaskManager.class).in(Scopes.SINGLETON);
        bind(WorkQueueSupplier.class).in(Scopes.SINGLETON);
        bind(TaskExecutionDetailsProjection.class).to(CassandraTaskExecutionDetailsProjection.class);
        bind(Hostname.class).in(Scopes.SINGLETON);
        bind(Hostname.class).toInstance(getHostname());
        bind(TaskManager.class).to(EventSourcingTaskManager.class);
        bind(WorkQueueSupplier.class).toInstance(workQueueSupplier);
    }

    private Hostname getHostname() {
        try {
            return new Hostname(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            throw new UnconfigurableHostname("Hostname can not be retrieved, unable to initialize the distributed task manager", e);
        }
    }
}
