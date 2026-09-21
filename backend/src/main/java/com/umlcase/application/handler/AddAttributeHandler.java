package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.AttributeAddedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.AddAttributeResponse;
import com.umlcase.infrastructure.web.dto.UmlAttributeDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AddAttributeHandler {

    private final UmlModelRepository repository;
    private final UmlEventPublisher publisher;

    public AddAttributeHandler(UmlModelRepository repository, UmlEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public AddAttributeResponse handle(UmlCommand.AddAttribute command) {
        UmlModel model = repository.loadForUpdate(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException(
                        "Proyecto no encontrado: " + command.projectId()));

        if (model.getVersion() != command.expectedVersion()) {
            throw new ModelVersionConflictException(
                    "Conflicto de versión. Esperada: " + command.expectedVersion()
                            + ", Actual: " + model.getVersion());
        }

        UmlAttribute attribute = model.addAttribute(
                command.classId(),
                command.attributeName(),
                command.attributeType(),
                command.visibility()
        );

        UmlModel savedModel = repository.save(model);

        publisher.publish(new AttributeAddedEvent(
                command.commandId().toString(),
                savedModel.getProjectId(),
                command.classId(),
                attribute.getId(),
                attribute.getName(),
                attribute.getType(),
                attribute.getVisibility().name(),
                attribute.getOrderIndex(),
                savedModel.getVersion()
        ));

        UmlAttributeDto dto = new UmlAttributeDto(
                attribute.getId(),
                attribute.getName(),
                attribute.getType(),
                attribute.getVisibility().name(),
                attribute.getOrderIndex()
        );

        return new AddAttributeResponse(
                command.commandId().toString(),
                command.classId(),
                dto,
                savedModel.getVersion()
        );
    }
}
