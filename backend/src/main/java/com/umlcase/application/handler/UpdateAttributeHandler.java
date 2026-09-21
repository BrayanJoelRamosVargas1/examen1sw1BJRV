package com.umlcase.application.handler;

import com.umlcase.application.command.UpdateAttributeCommand;
import com.umlcase.application.event.AttributeUpdatedEvent;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.model.UmlModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateAttributeHandler {

    private final UmlModelRepository repository;
    private final UmlEventPublisher publisher;

    public UpdateAttributeHandler(UmlModelRepository repository, UmlEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public com.umlcase.infrastructure.web.dto.UpdateAttributeResponse handle(UpdateAttributeCommand command) {
        UmlModel model = repository.loadForUpdate(command.projectId())
                .orElseThrow(() -> new com.umlcase.application.exception.ProjectNotFoundException("Proyecto no encontrado: " + command.projectId()));

        if (model.getVersion() != command.expectedVersion()) {
            throw new ModelVersionConflictException(
                    "Version mismatch. Expected: " + command.expectedVersion() + ", but was: " + model.getVersion());
        }

        UmlAttribute updatedAttribute = model.updateAttribute(
                command.classId(),
                command.attributeId(),
                command.name(),
                command.type(),
                command.visibility()
        );

        UmlModel savedModel = repository.save(model);

        AttributeUpdatedEvent event = AttributeUpdatedEvent.builder()
                .commandId(command.commandId())
                .projectId(command.projectId())
                .classId(command.classId())
                .attributeId(updatedAttribute.getId())
                .name(updatedAttribute.getName())
                .type(updatedAttribute.getType())
                .visibility(updatedAttribute.getVisibility())
                .orderIndex(updatedAttribute.getOrderIndex())
                .modelVersion(savedModel.getVersion())
                .build();

        publisher.publish(event);

        return new com.umlcase.infrastructure.web.dto.UpdateAttributeResponse(
                command.commandId(),
                command.classId(),
                new com.umlcase.infrastructure.web.dto.UmlAttributeDto(
                        updatedAttribute.getId(),
                        updatedAttribute.getName(),
                        updatedAttribute.getType(),
                        updatedAttribute.getVisibility().name(),
                        updatedAttribute.getOrderIndex()
                ),
                savedModel.getVersion()
        );
    }
}
