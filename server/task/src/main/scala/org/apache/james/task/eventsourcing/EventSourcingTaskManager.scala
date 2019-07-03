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

import com.google.common.annotations.VisibleForTesting
import javax.inject.Inject
import org.apache.james.eventsourcing.eventstore.EventStore
import org.apache.james.eventsourcing.{Command, CommandHandler, EventSourcingSystem, Subscriber}
import org.apache.james.task.Task.Result
import org.apache.james.task.TaskManager.Status
import org.apache.james.task.TaskManagerWorker.Listener
import org.apache.james.task._

import scala.annotation.tailrec

class Create(val id: TaskId, val task: Task) extends Command
class Cancel(val id: TaskId) extends Command

class EventSourcingTaskManager @Inject @VisibleForTesting private[eventsourcing](val eventStore: EventStore) extends TaskManager with Closeable {

  private val executionDetailsProjection = new TaskExecutionDetailsProjection
  private val recentTasksProjection = new RecentTasksProjection()
  private val worker: TaskManagerWorker = new SerialTaskManagerWorker
  private val workQueue: WorkQueue = WorkQueue.builder().worker(worker)
  val handlers: Set[CommandHandler[_]] = Set(
    new CreateCommandHandler(eventStore),
    new CancelCommandHandler(eventStore)
  )

  private def update(taskId: TaskId)(updater: TaskExecutionDetails => TaskExecutionDetails): Unit = {
    val aggregateId = TaskAggregateId(taskId)
    executionDetailsProjection
      .load(taskId)
      .map(updater)
      .foreach(executionDetailsProjection.update(aggregateId, _))
  }

  private def listener(taskWithId: TaskWithId) = {

    val updateTask = update(taskWithId.getId) _

    new Listener {
      override def started(): Unit = updateTask(_.start())

      override def completed(result: Result): Unit = updateTask(_.completed())

      override def failed(t: Throwable): Unit = updateTask(_.failed())

      override def failed(): Unit = updateTask(_.failed())

      override def cancelled(): Unit = updateTask(_.cancelEffectively())
    }
  }

  def workDispatcher: Subscriber = {
    case Created(aggregateId, _, task) =>
      val taskWithId = new TaskWithId(aggregateId.taskId, task)
      workQueue.submit(taskWithId, listener(taskWithId))
    case CancelRequested(aggregateId, _, _) =>
      workQueue.cancel(aggregateId.taskId)
    case _ =>
  }

  def detailsProjectionUpdater: Subscriber = {
    case created: Created =>
      executionDetailsProjection.update(created.getAggregateId, TaskExecutionDetails.from(created.task, created.aggregateId.taskId))
    case cancelRequested: CancelRequested =>
      update(cancelRequested.aggregateId.taskId)(_.cancelRequested())
    case _ =>
  }

  def recentTasksProjectionUpdater: Subscriber = {
    case Created(aggregateId, _, _) => recentTasksProjection.add(aggregateId.taskId)
    case _ =>
  }

  val subscribers: Set[Subscriber] = Set(detailsProjectionUpdater, workDispatcher, recentTasksProjectionUpdater)
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

  override def list: util.List[TaskExecutionDetails] = listScala.asJava

  override def list(status: TaskManager.Status): util.List[TaskExecutionDetails] = listScala.filter(details => details.getStatus == status).asJava

  private def listScala: List[TaskExecutionDetails] = recentTasksProjection.list().flatMap(executionDetailsProjection.load)

  override def cancel(id: TaskId): Unit = {
    val command = new Cancel(id)
    eventSourcingSystem.dispatch(command)
  }

  override def await(id: TaskId): TaskExecutionDetails = {
    @tailrec
    def rec(id: TaskId): TaskExecutionDetails = {
      val details = getExecutionDetails(id)
      if (Seq(Status.COMPLETED, Status.FAILED, Status.CANCELLED).contains(details.getStatus)) {
        details
      } else {
        Thread.sleep(500);
        rec(id)
      }
    }
    rec(id)
  }

  override def close(): Unit = {
    workQueue.close()
  }
}