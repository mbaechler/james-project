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
import org.apache.james.task.{Task, TaskId}

import scala.collection.JavaConverters._

case class InternalProjection(aggregateId: TaskAggregateId, status: Option[Event] = None, task: Option[Task] = None) {
  def apply(event: Event): InternalProjection = event match {
    case e: Created => copy(status = Some(e), task = Some(e.task))
    case e: CancelRequested => copy(status = Some(e))
    case _ => this
  }
}

class TaskAggregate private(val aggregateId: TaskAggregateId, private val history: History) {

  private val internalProjection = history.getEvents.asScala.foldLeft(InternalProjection(aggregateId))(_.apply(_))

  def create(task: Task): util.List[Event] = {
    publishEvents(history,
      Created(aggregateId, _, task))
  }

  def cancel(id: TaskId): util.List[Event] = {
    if (aggregateId.taskId.equals(id)) {
      publishEvents(history,
        CancelRequested(aggregateId, _, internalProjection.task.get))
    } else {
      Nil.asJava
    }
  }


  private def publishEvents(history: History, events: (EventId => Event)*): util.List[Event] =
    Stream.iterate(history.getNextEventId)(_.next())
      .zip(events)
      .map({ case (eventId, builder) => builder(eventId) })
      .asJava
}

object TaskAggregate {
  def fromHistory(aggregateId: TaskAggregateId, history: History) = new TaskAggregate(aggregateId, history)
}
