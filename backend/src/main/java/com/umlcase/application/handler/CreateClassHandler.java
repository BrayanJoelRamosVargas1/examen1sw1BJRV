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
    private final com.umlcase.infrastructure.persistence.repository.ProcessedCommandRepository processedCommandRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public CreateClassHandler(UmlModelRepository repository, UmlEventPublisher publisher,
                              com.umlcase.infrastructure.persistence.repository.ProcessedCommandRepository processedCommandRepository,
                              com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.repository = repository;
        this.publisher = publisher;
        this.processedCommandRepository = processedCommandRepository;
        this.objectMapper = objectMapper;
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
        String payloadStr = "CREATE_CLASS|" + command.className();
        String fingerprint = com.umlcase.application.port.out.CommandFingerprint.calculate("CREATE_CLASS", payloadStr);

        com.umlcase.infrastructure.persistence.entity.ProcessedCommandId processedCommandId = new com.umlcase.infrastructure.persistence.entity.ProcessedCommandId(command.projectId().toString(), command.commandId().toString());
        java.util.Optional<com.umlcase.infrastructure.persistence.entity.ProcessedCommand> existingReceiptOpt = processedCommandRepository.findById(processedCommandId);

        if (existingReceiptOpt.isPresent()) {
            com.umlcase.infrastructure.persistence.entity.ProcessedCommand existingReceipt = existingReceiptOpt.get();
            if (!existingReceipt.getRequestFingerprint().equals(fingerprint)) {
                throw new com.umlcase.application.exception.CommandIdReuseException("Reuso de commandId con diferente payload");
            }
            try {
                return objectMapper.readValue(existingReceipt.getResponsePayload(), CreateClassResponse.class);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Error parsing response payload", e);
            }
        }

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

        CreateClassResponse response = new CreateClassResponse(
                command.commandId().toString(),
                newClass.getId(),
                newClass.getName(),
                savedModel.getVersion()   // ← versión real post-flush
        );

        com.umlcase.infrastructure.persistence.entity.ProcessedCommand processedCommand = com.umlcase.infrastructure.persistence.entity.ProcessedCommand.builder()
                .projectId(command.projectId().toString())
                .commandId(command.commandId().toString())
                .commandType("CREATE_CLASS")
                .requestFingerprint(fingerprint)
                .modelVersion(savedModel.getVersion())
                .createdAt(java.time.LocalDateTime.now())
                .build();
        try {
            processedCommand.setResponsePayload(objectMapper.writeValueAsString(response));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Error serializing response payload", e);
        }
        processedCommandRepository.save(processedCommand);

        return response;
    }
}
