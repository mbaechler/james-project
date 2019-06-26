package org.apache.james.task.eventsourcing;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskId;

public class TaskExecutionDetailsProjection {
    private final Map<TaskId, TaskExecutionDetails> projections;

    public TaskExecutionDetailsProjection() {
        projections = new ConcurrentHashMap<>();
    }

    public Optional<TaskExecutionDetails> load(TaskId taskId) {
        return Optional.ofNullable(projections.get(taskId));
    }

    public void update(TaskAggregateId aggregateId, TaskExecutionDetails details) {
        projections.put(aggregateId.getTaskId(), details);
    }
}
