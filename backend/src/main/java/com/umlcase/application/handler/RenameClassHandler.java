package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.ClassRenamedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RenameClassResponse;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RenameClassHandler {

    private final UmlModelRepository repository;
    private final UmlEventPublisher publisher;

    public RenameClassHandler(UmlModelRepository repository, UmlEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public RenameClassResponse handle(UmlCommand.RenameClass command) {
        // 1. Obtener el modelo usando loadForUpdate para garantizar bloqueo de mutación
        UmlModel model = repository.loadForUpdate(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException(
                        "Proyecto no encontrado: " + command.projectId()));

        // 2. Validar versión optimista (expectedVersion)
        if (model.getVersion() != command.expectedVersion()) {
            throw new ModelVersionConflictException(
                    "Conflicto de versión. Esperada: " + command.expectedVersion()
                    + ", Actual: " + model.getVersion());
        }

        // 3. Modificar dominio
        model.renameClass(command.classId(), command.newName());

        // 4. Persistir con flush — JPA actualiza la fecha y el @Version incrementa
        UmlModel savedModel = repository.save(model);

        // 5. Publicar evento interno con la versión real persistida
        publisher.publish(new ClassRenamedEvent(
                command.commandId().toString(),
                savedModel.getProjectId(),
                command.classId(),
                command.newName(),
                savedModel.getVersion()
        ));

        // 6. Devolver respuesta
        return new RenameClassResponse(
                command.commandId().toString(),
                command.classId(),
                command.newName(),
                savedModel.getVersion()
        );
    }
}
