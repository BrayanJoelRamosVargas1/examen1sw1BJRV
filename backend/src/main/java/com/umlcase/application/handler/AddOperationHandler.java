package com.umlcase.application.handler;

import com.umlcase.application.command.AddOperationCommand;
import com.umlcase.application.event.OperationAddedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.model.UmlParameter;
import com.umlcase.domain.port.UmlModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddOperationHandler {

    private final UmlModelRepository repository;
    private final UmlEventPublisher eventPublisher;

    public AddOperationHandler(UmlModelRepository repository, UmlEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result handle(AddOperationCommand command) {
        UmlModel model = repository.loadForUpdate(command.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException("Model not found for project " + command.getProjectId()));

        if (model.getVersion() != command.getExpectedVersion()) {
            throw new ModelVersionConflictException("Conflicto de concurrencia al actualizar la clase.");
        }

        UmlClass targetClass = model.getClasses().stream()
                .filter(c -> c.getId().equals(command.getClassId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Class not found " + command.getClassId()));

        int maxOrder = targetClass.getOperations().stream()
                .mapToInt(UmlOperation::getOrderIndex)
                .max().orElse(-1);
        
        UmlOperation operation = new UmlOperation(
                java.util.UUID.randomUUID(),
                command.getName(),
                command.getReturnType(),
                command.getVisibility(),
                maxOrder + 1
        );

        if (command.getParameters() != null) {
            for (AddOperationCommand.ParameterData pd : command.getParameters()) {
                operation.addParameter(UmlParameter.create(pd.getName(), pd.getType()));
            }
        }

        targetClass.addOperation(operation);

        UmlModel savedModel = repository.save(model);

        OperationAddedEvent event = new OperationAddedEvent(
                command.getCommandId(),
                command.getProjectId(),
                command.getClassId(),
                operation.getId(),
                operation.getName(),
                operation.getReturnType(),
                operation.getVisibility(),
                operation.getOrderIndex(),
                operation.getParameters().stream().map(p -> new OperationAddedEvent.ParameterInfo(
                        p.getId(), p.getName(), p.getType(), operation.getParameters().indexOf(p)
                )).toList(),
                savedModel.getVersion()
        );

        eventPublisher.publish(event);
        return new Result(savedModel.getVersion(), operation);
    }

    public record Result(long newModelVersion, UmlOperation operation) {}
}
