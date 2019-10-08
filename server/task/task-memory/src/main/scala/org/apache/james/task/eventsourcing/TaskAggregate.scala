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

import java.util

import org.apache.james.eventsourcing.eventstore.History
import org.apache.james.eventsourcing.{Event, EventId}
import org.apache.james.task.Task.Result
import org.apache.james.task.TaskExecutionDetails.AdditionalInformation
import org.apache.james.task.TaskManager.Status
import org.apache.james.task.{Hostname, Task, TaskType}

import scala.collection.JavaConverters._

class TaskAggregate private(val aggregateId: TaskAggregateId, private val history: History) {

  history.getEvents.asScala.headOption match {
    case Some(Created(_, _, _, _)) =>
    case _ => throw new IllegalArgumentException("History must start with Created event")
  }

  private val currentStatus: Status = history
    .getEvents
    .asScala
    .foldLeft(DecisionProjection.empty)((decision, event) => decision.update(event))
    .status
    .get

  private implicit def toEventList(event: EventId => Event): util.List[Event] =
    toEventList(Some(event))

  private implicit def toEventList(maybeEvent: Option[EventId => Event]): util.List[Event] =
    maybeEvent.map(event => event(history.getNextEventId)).toSeq.asJava

  private[eventsourcing] def start(hostname: Hostname): util.List[Event] = {
    if (!currentStatus.isFinished)
      Started(aggregateId, _, hostname)
    else
      None
  }

  private[eventsourcing] def requestCancel(hostname: Hostname): util.List[Event] = {
    if (!currentStatus.isFinished)
      CancelRequested(aggregateId, _, hostname)
    else
      None
  }

  private[eventsourcing] def update(additionalInformation: AdditionalInformation): util.List[Event] = {
    currentStatus match {
      case Status.IN_PROGRESS => AdditionalInformationUpdated(aggregateId, _, additionalInformation)
      case Status.CANCEL_REQUESTED => AdditionalInformationUpdated(aggregateId, _, additionalInformation)
      case Status.COMPLETED => None
      case Status.FAILED => None
      case Status.WAITING => None
      case Status.CANCELLED => None
    }
  }

  private[eventsourcing] def complete(result: Result, taskType: TaskType, additionalInformation: Option[AdditionalInformation]): util.List[Event] = {
    if (!currentStatus.isFinished)
      Completed(aggregateId, _, result, taskType, additionalInformation)
    else
      None
  }

  private[eventsourcing] def fail(taskType : TaskType, additionalInformation: Option[AdditionalInformation]): util.List[Event] = {
    if (!currentStatus.isFinished)
      Failed(aggregateId, _, taskType, additionalInformation)
    else
      None
  }

  private[eventsourcing] def cancel(taskType: TaskType, additionalInformation: Option[AdditionalInformation]): util.List[Event] = {
    if (!currentStatus.isFinished)
      Cancelled(aggregateId, _, taskType, additionalInformation)
    else
      None
  }

}

object TaskAggregate {
  def fromHistory(aggregateId: TaskAggregateId, history: History): TaskAggregate = new TaskAggregate(aggregateId, history)

  def create(aggregateId: TaskAggregateId, task: Task, hostname: Hostname): util.List[Event] = {
    List[Event](Created(aggregateId, EventId.first(), task, hostname)).asJava
  }
}
