package org.apache.james.backends.cassandra.migration;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class MigrationTaskSerializationTest {

    private final CassandraSchemaVersionDAO cassandraSchemaVersionDAO = mock(CassandraSchemaVersionDAO.class);
    private final CassandraSchemaTransitions transitions = mock(CassandraSchemaTransitions.class);
    private final MigrationTask.Factory factory = target -> new MigrationTask(cassandraSchemaVersionDAO, transitions, target);
    private final JsonTaskSerializer taskSerializer = new JsonTaskSerializer(MigrationTaskDTO.MODULE.apply(factory));

    @Test
    void taskShouldBeSerializable() throws JsonProcessingException {
        MigrationTask task = factory.create(new SchemaVersion(12));
        assertThatJson(taskSerializer.serialize(task)).isEqualTo("{\"type\": \"cassandra-migration-task\", \"targetVersion\": 12}");
    }

    @Test
    void taskShouldBeDeserializable() throws IOException {
        MigrationTask task = factory.create(new SchemaVersion(12));
        assertThat(taskSerializer.deserialize("{\"type\": \"cassandra-migration-task\", \"targetVersion\": 12}"))
            .isEqualToComparingFieldByField(task);
    }

}