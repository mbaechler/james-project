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
package org.apache.james.task.eventsourcing.distributed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import org.apache.james.backends.rabbitmq.RabbitMQExtension;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.server.task.json.TestTask;
import org.apache.james.server.task.json.dto.TestTaskDTOModules;
import org.apache.james.task.CompletedTask;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskId;
import org.apache.james.task.TaskManagerWorker;
import org.apache.james.task.TaskType;
import org.apache.james.task.TaskWithId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

class RabbitMQWorkQueueTest {
    private static final TaskId TASK_ID = TaskId.fromString("2c7f4081-aa30-11e9-bf6c-2d3b9e84aafd");
    private static final TaskId TASK_ID_2 = TaskId.fromString("3c7f4081-aa30-11e9-bf6c-2d3b9e84aafd");
    private static final Task TASK = new CompletedTask();
    private static final Task TASK2 = new CompletedTask();
    private static final TaskWithId TASK_WITH_ID = new TaskWithId(TASK_ID, TASK);
    private static final TaskWithId TASK_WITH_ID_2 = new TaskWithId(TASK_ID_2, TASK2);

    @RegisterExtension
    static RabbitMQExtension rabbitMQExtension = RabbitMQExtension.singletonRabbitMQ();

    private RabbitMQWorkQueue testee;
    private ImmediateWorker worker;
    private JsonTaskSerializer serializer;

    private static class ImmediateWorker implements TaskManagerWorker {

        ConcurrentLinkedQueue<TaskWithId> tasks = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Task.Result> results = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<TaskId> failedTasks = new ConcurrentLinkedQueue<>();

        @Override
        public Mono<Task.Result> executeTask(TaskWithId taskWithId) {
            tasks.add(taskWithId);
            return Mono.fromCallable(() -> taskWithId.getTask().run())
                .doOnNext(result -> results.add(result))
                .subscribeOn(Schedulers.boundedElastic());
        }

        @Override
        public void cancelTask(TaskId taskId) {
        }

        @Override
        public void fail(TaskId taskId, TaskType taskType, Optional<TaskExecutionDetails.AdditionalInformation> additionalInformation, Throwable reason) {
            failedTasks.add(taskId);
        }

        @Override
        public void close() throws IOException {
        }
    }

    @BeforeEach
    void setUp() {
        worker = new ImmediateWorker();
        serializer = new JsonTaskSerializer(TestTaskDTOModules.COMPLETED_TASK_MODULE);
        testee = new RabbitMQWorkQueue(worker, rabbitMQExtension.getRabbitConnectionPool(), serializer);
        testee.start();
    }

    @AfterEach
    void tearDown() {
        testee.close();
    }

    @Test
    void workqueueShouldConsumeSubmittedTask() {
        testee.submit(TASK_WITH_ID);
        await().atMost(FIVE_HUNDRED_MILLISECONDS).until(() -> !worker.results.isEmpty());
        assertThat(worker.tasks).containsExactly(TASK_WITH_ID);
        assertThat(worker.results).containsExactly(Task.Result.COMPLETED);
    }

    @Test
    void workqueueShouldConsumeTwoSubmittedTask() {
        testee.submit(TASK_WITH_ID);
        testee.submit(TASK_WITH_ID_2);
        await().atMost(FIVE_HUNDRED_MILLISECONDS).until(() -> worker.results.size() == 2);
        assertThat(worker.tasks).containsExactly(TASK_WITH_ID, TASK_WITH_ID_2);
        assertThat(worker.results).allSatisfy(result -> assertThat(result).isEqualTo(Task.Result.COMPLETED));
    }

    @Test
    void givenTwoWorkQueuesOnlyTheFirstOneIsConsumingTasks() {
        testee.submit(TASK_WITH_ID);

        ImmediateWorker otherTaskManagerWorker = new ImmediateWorker();
        try (RabbitMQWorkQueue otherWorkQueue = new RabbitMQWorkQueue(otherTaskManagerWorker, rabbitMQExtension.getRabbitConnectionPool(), serializer)) {
            otherWorkQueue.start();

            IntStream.range(0, 9)
                .forEach(ignoredIndex -> testee.submit(TASK_WITH_ID_2));

            await().atMost(FIVE_HUNDRED_MILLISECONDS).until(() -> worker.results.size() == 10);
            assertThat(otherTaskManagerWorker.tasks).isEmpty();
        }
    }

    @Test
    void givenANonDeserializableTaskItShouldBeFlaggedAsFailedAndItDoesNotPreventFollowingTasks() throws InterruptedException {
        Task task = new TestTask(42);
        TaskId taskId = TaskId.fromString("4bf6d081-aa30-11e9-bf6c-2d3b9e84aafd");
        TaskWithId taskWithId = new TaskWithId(taskId, task);

        ImmediateWorker otherTaskManagerWorker = new ImmediateWorker();
        JsonTaskSerializer otherTaskSerializer = new JsonTaskSerializer(TestTaskDTOModules.TEST_TYPE);
        try (RabbitMQWorkQueue otherWorkQueue = new RabbitMQWorkQueue(otherTaskManagerWorker, rabbitMQExtension.getRabbitConnectionPool(), otherTaskSerializer)) {
            //wait to be sur that the first workqueue has subscribed as an exclusive consumer of the RabbitMQ queue.
            Thread.sleep(200);
            otherWorkQueue.start();

            otherWorkQueue.submit(taskWithId);

            await().atMost(FIVE_HUNDRED_MILLISECONDS).until(() -> worker.failedTasks.size() == 1);
            assertThat(worker.failedTasks).containsExactly(taskWithId.getId());

            testee.submit(TASK_WITH_ID);
            await().atMost(FIVE_HUNDRED_MILLISECONDS).until(() -> worker.results.size() == 1);
            assertThat(worker.tasks).containsExactly(TASK_WITH_ID);
        }
    }
}
