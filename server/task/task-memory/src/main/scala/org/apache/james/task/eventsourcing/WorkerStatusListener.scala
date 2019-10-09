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

import java.util.Optional

import org.apache.james.eventsourcing.EventSourcingSystem
import org.apache.james.task.Task.Result
import org.apache.james.task.eventsourcing.TaskCommand._
import org.apache.james.task.{TaskExecutionDetails, TaskId, TaskManagerWorker, TaskType}

import scala.compat.java8.OptionConverters._

case class WorkerStatusListener(eventSourcingSystem: EventSourcingSystem) extends TaskManagerWorker.Listener {

  override def started(taskId: TaskId): Unit = eventSourcingSystem.dispatch(Start(taskId))

  override def completed(taskId: TaskId, result: Result, taskType: TaskType, additionalInformation: Optional[TaskExecutionDetails.AdditionalInformation]): Unit =
    eventSourcingSystem.dispatch(Complete(taskId, result, taskType, additionalInformation.asScala))

  override def failed(taskId: TaskId, taskType: TaskType, additionalInformation: Optional[TaskExecutionDetails.AdditionalInformation], t: Throwable): Unit =
    eventSourcingSystem.dispatch(Fail(taskId, taskType, additionalInformation.asScala))

  override def failed(taskId: TaskId, taskType: TaskType, additionalInformation: Optional[TaskExecutionDetails.AdditionalInformation]): Unit =
    eventSourcingSystem.dispatch(Fail(taskId, taskType, additionalInformation.asScala))

  override def cancelled(taskId: TaskId, taskType: TaskType,additionalInformation: Optional[TaskExecutionDetails.AdditionalInformation]): Unit =
    eventSourcingSystem.dispatch(Cancel(taskId, taskType, additionalInformation.asScala ))

  override def updated(taskId: TaskId, taskType: TaskType, additionalInformation: TaskExecutionDetails.AdditionalInformation): Unit = ???
}