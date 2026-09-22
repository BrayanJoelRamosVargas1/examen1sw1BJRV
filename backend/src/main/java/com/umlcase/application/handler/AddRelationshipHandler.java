package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipAddedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.in.AddRelationshipUseCase;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.port.UmlModelRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddRelationshipHandler implements AddRelationshipUseCase {

    private final UmlModelRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public AddRelationshipHandler(UmlModelRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public RelationshipAddedEvent handle(UmlCommand.AddRelationship command) {
        UmlModel model = repository.findByProjectId(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Proyecto no encontrado"));

        if (model.getVersion() != command.expectedVersion()) {
            throw new ModelVersionConflictException(
                    "Conflicto de concurrencia. Versión esperada: " + command.expectedVersion() +
                            ", Versión actual: " + model.getVersion());
        }

        UmlRelationship relationship = UmlRelationship.create(
                command.type(),
                command.sourceClassId(),
                command.targetClassId(),
                command.sourceMultiplicity(),
                command.targetMultiplicity()
        );

        model.addRelationship(relationship);

        UmlModel savedModel = repository.save(model);

        RelationshipAddedEvent event = new RelationshipAddedEvent(
                command.commandId(),
                savedModel.getProjectId(),
                relationship.getId(),
                relationship.getSourceClassId(),
                relationship.getTargetClassId(),
                relationship.getType(),
                relationship.getSourceMultiplicity(),
                relationship.getTargetMultiplicity(),
                savedModel.getVersion()
        );

        eventPublisher.publishEvent(event);

        return event;
    }
}
