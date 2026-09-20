package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.ClassCreatedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.CreateClassResponse;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateClassHandler {

    private final UmlModelRepository repository;
    private final UmlEventPublisher publisher;

    public CreateClassHandler(UmlModelRepository repository, UmlEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Crea una clase en el modelo UML.
     *
     * Frontera transaccional: @Transactional cubre load → validate → addClass →
     * saveAndFlush → publishEvent → COMMIT → (AFTER_COMMIT) → STOMP.
     *
     * La versión del evento y la respuesta provienen del modelo persistido real
     * (JPA @Version post-flush), nunca de un cálculo manual version+1.
     *
     * @return CreateClassResponse con classId, className y modelVersion real
     */
    @Transactional
    public CreateClassResponse handle(UmlCommand.CreateClass command) {
        // 1. Obtener el modelo por projectId
        UmlModel model = repository.findByProjectId(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException(
                        "Proyecto no encontrado: " + command.projectId()));

        // 2. Validar versión optimista (expectedVersion)
        if (model.getVersion() != command.expectedVersion()) {
            throw new ModelVersionConflictException(
                    "Conflicto de versión. Esperada: " + command.expectedVersion()
                    + ", Actual: " + model.getVersion());
        }

        // 3. Modificar dominio
        UmlClass newClass = UmlClass.create(command.className());
        model.addClass(newClass);

        // 4. Persistir con flush — JPA @Version incrementa la versión realmente
        UmlModel savedModel = repository.save(model);

        // 5. Publicar evento interno con la versión real persistida
        //    (NO con command.expectedVersion()+1 manual)
        publisher.publish(new ClassCreatedEvent(
                command.commandId().toString(),
                savedModel.getProjectId(),
                newClass.getId(),
                newClass.getName(),
                savedModel.getVersion()   // ← versión real post-flush
        ));

        // 6. Devolver respuesta — la transacción hace commit al salir del método
        //    AFTER_COMMIT dispara StompUmlEventListener → SimpMessagingTemplate
        return new CreateClassResponse(
                command.commandId().toString(),
                newClass.getId(),
                newClass.getName(),
                savedModel.getVersion()   // ← versión real post-flush
        );
    }
}
