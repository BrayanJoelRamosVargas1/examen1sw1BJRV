package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipUpdatedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.in.UpdateRelationshipUseCase;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateRelationshipHandler implements UpdateRelationshipUseCase {

    private final UmlModelRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateRelationshipHandler(UmlModelRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public RelationshipUpdatedEvent handle(UmlCommand.UpdateRelationship command) {
        UmlModel model = repository.findByProjectId(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Proyecto no encontrado"));

        if (model.getVersion() != command.expectedVersion()) {
            throw new ModelVersionConflictException(
                    "Conflicto de concurrencia. Versión esperada: " + command.expectedVersion() +
                            ", Versión actual: " + model.getVersion());
        }

        model.updateRelationship(
                command.relationshipId(),
                command.type(),
                command.sourceMultiplicity(),
                command.targetMultiplicity()
        );

        // Actualizamos la versión del modelo para disparar el Optimistic Locking
        UmlModel savedModel = repository.save(model);

        RelationshipUpdatedEvent event = new RelationshipUpdatedEvent(
                command.commandId(),
                savedModel.getProjectId(),
                command.relationshipId(),
                command.type(),
                command.sourceMultiplicity(),
                command.targetMultiplicity(),
                savedModel.getVersion()
        );

        eventPublisher.publishEvent(event);

        return event;
    }
}
