/** **************************************************************
  * Licensed to the Apache Software Foundation (ASF) under one   *
  * or more contributor license agreements.  See the NOTICE file *
  * distributed with this work for additional information        *
  * regarding copyright ownership.  The ASF licenses this file   *
  * to you under the Apache License, Version 2.0 (the            *
  * "License"); you may not use this file except in compliance   *
  * with the License.  You may obtain a copy of the License at   *
  * *
  * http://www.apache.org/licenses/LICENSE-2.0                 *
  * *
  * Unless required by applicable law or agreed to in writing,   *
  * software distributed under the License is distributed on an  *
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
  * KIND, either express or implied.  See the License for the    *
  * specific language governing permissions and limitations      *
  * under the License.                                           *
  * ***************************************************************/
package org.apache.james.task.eventsourcing

import java.io.Closeable
import java.util
import java.util.function.Consumer

import com.google.common.annotations.VisibleForTesting
import javax.inject.Inject
import org.apache.james.eventsourcing.eventstore.EventStore
import org.apache.james.eventsourcing.{Command, CommandHandler, EventSourcingSystem, Subscriber}
import org.apache.james.task.TaskManagerWorker.Listener
import org.apache.james.task.{MemoryTaskManagerWorker, Task, TaskExecutionDetails, TaskId, TaskManager, TaskManagerWorker, TaskNotFoundException, TaskWithId, WorkQueue}

class Create(val id: TaskId, val task: Task) extends Command

class EventSourcingTaskManager @Inject @VisibleForTesting private[eventsourcing](val eventStore: EventStore) extends TaskManager with Closeable {
  private val executionDetailsProjection = new TaskExecutionDetailsProjection
  private val worker: TaskManagerWorker = new MemoryTaskManagerWorker
  private val workQueue: WorkQueue = WorkQueue.builder().worker(dispatchToWorker).listener(workQueueProjectionUpdater)
  val handlers: Set[CommandHandler[_]] = Set(new CreateCommandHandler(eventStore))

  private def dispatchToWorker(taskWithId: TaskWithId) = {
    val aggregateId = TaskAggregateId(taskWithId.getId)

    def update(updater: TaskExecutionDetails => TaskExecutionDetails): Unit = {
      executionDetailsProjection
        .load(taskWithId.getId)
        .map(updater)
        .foreach(executionDetailsProjection.update(aggregateId, _))
    }
    worker.executeTask(taskWithId, new Listener {
      override def started(): Unit = update(_.start())

      override def completed(): Unit = update(_.completed())

      override def failed(t: Throwable): Unit = update(_.failed())

      override def failed(): Unit = update(_.failed())

      override def cancelled(): Unit = update(_.cancelEffectively())
    })
  }

  def workDispatcher: Subscriber = {
    case Created(aggregateId, _, task) => workQueue.submit(new TaskWithId(aggregateId.taskId, task))
    case _ =>
  }

  def projectionUpdater: Subscriber = {
    case detailsChanged: DetailsChanged =>
      executionDetailsProjection.update(detailsChanged.getAggregateId, detailsChanged.details)
    case _ =>
  }

  def workQueueProjectionUpdater: Consumer[WorkQueue.Event] = event => {
    executionDetailsProjection
      .load(event.id)
      .foreach(details => event.status match {
          case WorkQueue.Status.CANCELLED => executionDetailsProjection.update(TaskAggregateId(event.id), details.cancelEffectively())
          case _ =>
      })
  }

  val subscribers: Set[Subscriber] = Set(projectionUpdater, workDispatcher)
  import scala.collection.JavaConverters._
  private val eventSourcingSystem = new EventSourcingSystem(handlers.asJava, subscribers.asJava, eventStore)
  def stop(): Unit = {
  }

  override def submit(task: Task): TaskId = {
    val taskId = TaskId.generateTaskId
    val command = new Create(taskId, task)
    eventSourcingSystem.dispatch(command)
    taskId
  }

  override def getExecutionDetails(id: TaskId): TaskExecutionDetails = executionDetailsProjection.load(id)
    .getOrElse(throw new TaskNotFoundException())

  override def list: util.List[TaskExecutionDetails] = Nil.asJava

  override def list(status: TaskManager.Status): util.List[TaskExecutionDetails] = Nil.asJava

  override def cancel(id: TaskId): Unit = {
  }

  override def await(id: TaskId): TaskExecutionDetails = null

  override def close(): Unit = {
    workQueue.close()
  }
}