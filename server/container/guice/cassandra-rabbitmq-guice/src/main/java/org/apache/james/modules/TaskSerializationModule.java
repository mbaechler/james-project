package org.apache.james.modules;

import org.apache.james.backends.cassandra.migration.MigrationTask;
import org.apache.james.backends.cassandra.migration.MigrationTaskDTO;
import org.apache.james.eventsourcing.eventstore.cassandra.dto.EventDTOModule;
import org.apache.james.rrt.cassandra.CassandraMappingsSourcesDAO;
import org.apache.james.rrt.cassandra.migration.MappingsSourcesMigration;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.server.task.json.dto.TaskDTOModule;
import org.apache.james.task.eventsourcing.distributed.TasksSerializationModule;
import org.apache.james.webadmin.service.CassandraMappingsSolveInconsistenciesTask;
import org.apache.mailbox.tools.indexer.FullReindexingTask;
import org.apache.mailbox.tools.indexer.ReIndexerPerformer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.ProvidesIntoSet;

public class TaskSerializationModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @ProvidesIntoSet
    public EventDTOModule<?, ?> taskCreatedSerialization(JsonTaskSerializer jsonTaskSerializer) {
        return TasksSerializationModule.CREATED.apply(jsonTaskSerializer);
    }

    @ProvidesIntoSet
    public EventDTOModule<?, ?> taskStartedSerialization(JsonTaskSerializer jsonTaskSerializer) {
        return TasksSerializationModule.STARTED.apply(jsonTaskSerializer);
    }

    @ProvidesIntoSet
    public EventDTOModule<?, ?> taskCancelRequestedSerialization(JsonTaskSerializer jsonTaskSerializer) {
        return TasksSerializationModule.CANCEL_REQUESTED.apply(jsonTaskSerializer);
    }

    @ProvidesIntoSet
    public EventDTOModule<?, ?> taskCancelledSerialization(JsonTaskSerializer jsonTaskSerializer) {
        return TasksSerializationModule.CANCELLED.apply(jsonTaskSerializer);
    }

    @ProvidesIntoSet
    public EventDTOModule<?, ?> taskCompletedSerialization(JsonTaskSerializer jsonTaskSerializer) {
        return TasksSerializationModule.COMPLETED.apply(jsonTaskSerializer);
    }

    @ProvidesIntoSet
    public EventDTOModule<?, ?> taskFailedSerialization(JsonTaskSerializer jsonTaskSerializer) {
        return TasksSerializationModule.FAILED.apply(jsonTaskSerializer);
    }

    @ProvidesIntoSet
    public TaskDTOModule<?, ?> fullReindexTask(ReIndexerPerformer performer) {
        return FullReindexingTask.module(performer);
    }

    @ProvidesIntoSet
    public TaskDTOModule<?, ?> cassandraMappingsSolveInconsistenciesTask(MappingsSourcesMigration migration, CassandraMappingsSourcesDAO dao) {
        return CassandraMappingsSolveInconsistenciesTask.module(migration, dao);
    }

    @ProvidesIntoSet
    public TaskDTOModule<?, ?> migrationTask(MigrationTask.Factory factory) {
        return MigrationTaskDTO.module(factory);
    }
}