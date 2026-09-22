package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.AttributeRemovedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RemoveAttributeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveAttributeHandler {
    private final UmlModelRepository repository;
    private final UmlEventPublisher publisher;

    public RemoveAttributeHandler(UmlModelRepository repository, UmlEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public RemoveAttributeResponse handle(UmlCommand.RemoveAttribute command) {
        UmlModel model = repository.loadForUpdate(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Proyecto no encontrado: " + command.projectId()));
        if (model.getVersion() != command.expectedVersion()) {
            throw new ModelVersionConflictException(
                    "Conflicto de versión. Esperada: " + command.expectedVersion() + ", Actual: " + model.getVersion());
        }

        model.removeAttribute(command.classId(), command.attributeId());
        UmlModel savedModel = repository.save(model);
        publisher.publish(new AttributeRemovedEvent(
                command.commandId(), command.projectId(), command.classId(), command.attributeId(), savedModel.getVersion()));

        return new RemoveAttributeResponse(
                command.commandId(), command.classId(), command.attributeId(), savedModel.getVersion());
    }
}
