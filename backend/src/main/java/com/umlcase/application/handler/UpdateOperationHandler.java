package com.umlcase.application.handler;

import com.umlcase.application.command.UpdateOperationCommand;
import com.umlcase.application.event.OperationUpdatedEvent;
import com.umlcase.application.port.in.UpdateOperationUseCase;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlParameter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Service
public class UpdateOperationHandler implements UpdateOperationUseCase {

    private final UmlModelRepository repository;
    private final UmlEventPublisher eventPublisher;

    public UpdateOperationHandler(UmlModelRepository repository, UmlEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OperationUpdatedEvent handle(UpdateOperationCommand command) {
        UmlModel model = repository.loadForUpdate(command.projectId())
                .orElseThrow(() -> new com.umlcase.application.exception.ProjectNotFoundException("Project not found: " + command.projectId()));

        if (model.getVersion() != command.expectedVersion()) {
            throw new com.umlcase.application.exception.ModelVersionConflictException(
                    "Conflicto de versión: esperada " + command.expectedVersion() + ", actual " + model.getVersion()
            );
        }

        UmlClass cls = model.getClasses().stream()
                .filter(c -> c.getId().equals(command.classId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Class not found"));

        // Convert command parameters to domain objects, preserving ID if present
        List<UmlParameter> domainParams = new ArrayList<>();
        int index = 0;
        for (UpdateOperationCommand.ParameterData paramData : command.parameters()) {
            UUID paramId = paramData.id() != null ? paramData.id() : UUID.randomUUID();
            UmlParameter newParam = new UmlParameter(paramId, paramData.name(), paramData.type(), index++);
            domainParams.add(newParam);
        }

        model.updateOperation(command.classId(), command.operationId(), command.name(), command.returnType(), command.visibility(), domainParams);

        UmlModel savedModel = repository.save(model);

        var finalOp = cls.getOperations().stream()
                .filter(o -> o.getId().equals(command.operationId()))
                .findFirst()
                .orElseThrow();

        OperationUpdatedEvent event = new OperationUpdatedEvent(
                "OPERATION_UPDATED",
                command.commandId(),
                command.projectId(),
                command.classId(),
                command.operationId(),
                finalOp.getName(),
                finalOp.getReturnType(),
                finalOp.getVisibility(),
                finalOp.getOrderIndex(),
                finalOp.getParameters().stream()
                        .map(p -> new OperationUpdatedEvent.ParameterData(p.getId(), p.getName(), p.getType(), p.getOrderIndex()))
                        .collect(Collectors.toList()),
                savedModel.getVersion()
        );

        eventPublisher.publish(event);

        return event;
    }
}
