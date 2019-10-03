package org.apache.james.modules;

import org.apache.james.backends.cassandra.migration.MigrationTask;
import org.apache.james.backends.cassandra.migration.MigrationTaskDTO;
import org.apache.james.eventsourcing.eventstore.cassandra.dto.EventDTOModule;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.rrt.cassandra.CassandraMappingsSourcesDAO;
import org.apache.james.rrt.cassandra.migration.MappingsSourcesMigration;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.server.task.json.dto.TaskDTOModule;
import org.apache.james.task.eventsourcing.distributed.TasksSerializationModule;
import org.apache.james.webadmin.service.CassandraMappingsSolveInconsistenciesTask;
import org.apache.james.webadmin.service.DeleteMailsFromMailQueueTaskDTO;
import org.apache.james.webadmin.service.ReprocessingAllMailsTaskDTO;
import org.apache.james.webadmin.service.ReprocessingService;
import org.apache.mailbox.tools.indexer.FullReindexingTask;
import org.apache.mailbox.tools.indexer.MessageIdReIndexingTask;
import org.apache.mailbox.tools.indexer.MessageIdReindexingTaskDTO;
import org.apache.mailbox.tools.indexer.ReIndexerPerformer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.ProvidesIntoSet;
import org.apache.mailbox.tools.indexer.SingleMessageReindexingTask;
import org.apache.mailbox.tools.indexer.SingleMessageReindexingTaskDTO;

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
    public TaskDTOModule<?, ?> deleteMailsFromMailQueueTask(MailQueueFactory<?> mailQueueFactory) {
        return DeleteMailsFromMailQueueTaskDTO.module((MailQueueFactory<ManageableMailQueue>) mailQueueFactory);
    }

    @ProvidesIntoSet
    public TaskDTOModule<?, ?> reprocessingAllMailsTask(ReprocessingService reprocessingService) {
        return ReprocessingAllMailsTaskDTO.module(reprocessingService);
    }

    @ProvidesIntoSet
    public TaskDTOModule<?, ?> singleMessageReindexingTask(SingleMessageReindexingTask.Factory factory) {
        return SingleMessageReindexingTaskDTO.module(factory);
    }

    @ProvidesIntoSet
    public TaskDTOModule<?, ?> messageIdReindexingTask(MessageIdReIndexingTask.Factory factory) {
        return MessageIdReindexingTaskDTO.module(factory);
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
