package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipRemovedEvent;
import com.umlcase.application.port.in.RemoveRelationshipUseCase;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.domain.model.UmlModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RemoveRelationshipHandler implements RemoveRelationshipUseCase {

    private final UmlModelRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public RemoveRelationshipHandler(UmlModelRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RelationshipRemovedEvent handle(UmlCommand.RemoveRelationship command) {
        UmlModel model = repository.findByProjectId(command.projectId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el proyecto con id " + command.projectId()));

        if (model.getVersion() != command.expectedVersion()) {
            throw new com.umlcase.application.exception.ModelVersionConflictException(
                    "Conflicto de concurrencia: la versión esperada era " + command.expectedVersion() +
                    " pero la versión actual es " + model.getVersion()
            );
        }

        // Eliminación validando existencia
        model.removeRelationship(command.relationshipId());
        UmlModel savedModel = repository.save(model);

        RelationshipRemovedEvent event = new RelationshipRemovedEvent(
                command.commandId(),
                savedModel.getProjectId(),
                command.relationshipId(),
                savedModel.getVersion()
        );

        eventPublisher.publishEvent(event);
        return event;
    }
}
