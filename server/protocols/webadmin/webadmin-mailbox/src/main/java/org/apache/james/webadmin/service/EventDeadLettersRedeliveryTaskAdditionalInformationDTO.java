package org.apache.james.webadmin.service;

import java.util.Optional;

import org.apache.james.json.DTOModule;
import org.apache.james.mailbox.events.EventDeadLetters;
import org.apache.james.mailbox.events.Group;
import org.apache.james.server.task.json.dto.AdditionalInformationDTO;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.lambdas.Throwing;

public class EventDeadLettersRedeliveryTaskAdditionalInformationDTO implements AdditionalInformationDTO {

    public static final AdditionalInformationDTOModule<EventDeadLettersRedeliveryTaskAdditionalInformation, EventDeadLettersRedeliveryTaskAdditionalInformationDTO> MODULE =
        DTOModule
            .forDomainObject(EventDeadLettersRedeliveryTaskAdditionalInformation.class)
            .convertToDTO(EventDeadLettersRedeliveryTaskAdditionalInformationDTO.class)
            .toDomainObjectConverter(EventDeadLettersRedeliveryTaskAdditionalInformationDTO::fromDTO)
            .toDTOConverter(EventDeadLettersRedeliveryTaskAdditionalInformationDTO::toDTO)
            .typeName("EventDeadLettersRedeliverTask")
            .withFactory(AdditionalInformationDTOModule::new);

    private static EventDeadLettersRedeliveryTaskAdditionalInformationDTO toDTO(EventDeadLettersRedeliveryTaskAdditionalInformation domainObject, String typeName) {
        return new EventDeadLettersRedeliveryTaskAdditionalInformationDTO(
            domainObject.getSuccessfulRedeliveriesCount(),
            domainObject.getFailedRedeliveriesCount(),
            domainObject.getGroup(),
            domainObject.getInsertionId());
    }

    private static EventDeadLettersRedeliveryTaskAdditionalInformation fromDTO(EventDeadLettersRedeliveryTaskAdditionalInformationDTO dto) {
        return new EventDeadLettersRedeliveryTaskAdditionalInformation(
            dto.successfulRedeliveriesCount,
            dto.failedRedeliveriesCount,
            dto.group.map(Throwing.function(Group::deserialize).sneakyThrow()),
            dto.insertionId.map(EventDeadLetters.InsertionId::of));
    }


    private final long successfulRedeliveriesCount;
    private final long failedRedeliveriesCount;
    private final Optional<String> group;
    private final Optional<String> insertionId;

    public EventDeadLettersRedeliveryTaskAdditionalInformationDTO(@JsonProperty("successfulRedeliveriesCount") long successfulRedeliveriesCount,
                                                                  @JsonProperty("failedRedeliveriesCount") long failedRedeliveriesCount,
                                                                  @JsonProperty("group") Optional<String> group,
                                                                  @JsonProperty("insertionId") Optional<String> insertionId
    ) {
        this.successfulRedeliveriesCount = successfulRedeliveriesCount;
        this.failedRedeliveriesCount = failedRedeliveriesCount;
        this.group = group;
        this.insertionId = insertionId;
    }


    public long getSuccessfulRedeliveriesCount() {
        return successfulRedeliveriesCount;
    }

    public long getFailedRedeliveriesCount() {
        return failedRedeliveriesCount;
    }

    public Optional<String> getGroup() {
        return group;
    }

    public Optional<String> getInsertionId() {
        return insertionId;
    }
}