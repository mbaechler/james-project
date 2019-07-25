package org.apache.james.backends.cassandra.migration;

import java.util.function.Function;

import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.json.DTOModule;
import org.apache.james.server.task.json.dto.TaskDTO;
import org.apache.james.server.task.json.dto.TaskDTOModule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;

class MigrationTaskDTO implements TaskDTO {

    @VisibleForTesting static final Function<MigrationTask.Factory, TaskDTOModule<MigrationTask, MigrationTaskDTO>> MODULE =
            factory -> DTOModule.forDomainObject(MigrationTask.class)
                .convertToDTO(MigrationTaskDTO.class)
                .toDomainObjectConverter(dto -> factory.create(new SchemaVersion(dto.targetVersion)))
                .toDTOConverter((task, type) -> new MigrationTaskDTO(type, task.getTargetVersion().getValue()))
                .typeName("cassandra-migration-task")
                .withFactory(TaskDTOModule::new);


    private final String type;
    private final int targetVersion;

    MigrationTaskDTO(@JsonProperty("type") String type, @JsonProperty("targetVersion") int targetVersion) {
        this.type = type;
        this.targetVersion = targetVersion;
    }

    @Override
    public String getType() {
        return type;
    }

    public int getTargetVersion() {
        return targetVersion;
    }
}
