package org.apache.james.task.eventsourcing

import org.apache.james.task.TaskId

import scala.collection.mutable

class RecentTasksProjection() {
  private val tasks = new mutable.ListBuffer[TaskId]

  def list(): List[TaskId] = tasks.result()

  def add(taskId: TaskId): Unit = tasks.append(taskId)
}
