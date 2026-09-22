package com.umlcase.application.handler;

import com.umlcase.application.command.RemoveOperationCommand;
import com.umlcase.application.event.OperationRemovedEvent;
import com.umlcase.application.port.in.RemoveOperationUseCase;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveOperationHandler implements RemoveOperationUseCase {

    private final UmlModelRepository modelRepository;
    private final UmlEventPublisher eventPublisher;

    public RemoveOperationHandler(UmlModelRepository modelRepository, UmlEventPublisher eventPublisher) {
        this.modelRepository = modelRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public OperationRemovedEvent handle(RemoveOperationCommand command) {
        UmlModel model = modelRepository.findByProjectId(command.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found with ID: " + command.projectId()));

        if (model.getVersion() != command.expectedVersion()) {
            throw new com.umlcase.application.exception.ModelVersionConflictException(
                    "Conflict: Expected version " + command.expectedVersion() + " but found " + model.getVersion()
            );
        }

        model.removeOperation(command.classId(), command.operationId());

        UmlModel savedModel = modelRepository.save(model);

        OperationRemovedEvent event = new OperationRemovedEvent(
                "OPERATION_REMOVED",
                command.commandId(),
                command.projectId(),
                command.classId(),
                command.operationId(),
                savedModel.getVersion()
        );

        eventPublisher.publish(event);

        return event;
    }
}
