package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.AttributeAddedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.AddAttributeResponse;
import com.umlcase.infrastructure.web.dto.UmlAttributeDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AddAttributeHandler {

    private final UmlModelRepository repository;
    private final UmlEventPublisher publisher;
    private final com.umlcase.infrastructure.persistence.repository.ProcessedCommandRepository processedCommandRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public AddAttributeHandler(UmlModelRepository repository, UmlEventPublisher publisher,
                               com.umlcase.infrastructure.persistence.repository.ProcessedCommandRepository processedCommandRepository,
                               com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.repository = repository;
        this.publisher = publisher;
        this.processedCommandRepository = processedCommandRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AddAttributeResponse handle(UmlCommand.AddAttribute command) {
        String payloadStr = "ADD_ATTRIBUTE|" + command.classId() + "|" + command.attributeName() + "|" + command.attributeType() + "|" + command.visibility();
        String fingerprint = com.umlcase.application.port.out.CommandFingerprint.calculate("ADD_ATTRIBUTE", payloadStr);

        com.umlcase.infrastructure.persistence.entity.ProcessedCommandId processedCommandId = new com.umlcase.infrastructure.persistence.entity.ProcessedCommandId(command.projectId().toString(), command.commandId().toString());
        java.util.Optional<com.umlcase.infrastructure.persistence.entity.ProcessedCommand> existingReceiptOpt = processedCommandRepository.findById(processedCommandId);

        if (existingReceiptOpt.isPresent()) {
            com.umlcase.infrastructure.persistence.entity.ProcessedCommand existingReceipt = existingReceiptOpt.get();
            if (!existingReceipt.getRequestFingerprint().equals(fingerprint)) {
                throw new com.umlcase.application.exception.CommandIdReuseException("Reuso de commandId con diferente payload");
            }
            try {
                return objectMapper.readValue(existingReceipt.getResponsePayload(), AddAttributeResponse.class);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Error parsing response payload", e);
            }
        }

        UmlModel model = repository.loadForUpdate(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException(
                        "Proyecto no encontrado: " + command.projectId()));

        if (model.getVersion() != command.expectedVersion()) {
            throw new ModelVersionConflictException(
                    "Conflicto de versión. Esperada: " + command.expectedVersion()
                            + ", Actual: " + model.getVersion());
        }

        UmlAttribute attribute = model.addAttribute(
                command.classId(),
                command.attributeName(),
                command.attributeType(),
                command.visibility()
        );

        UmlModel savedModel = repository.save(model);

        publisher.publish(new AttributeAddedEvent(
                command.commandId().toString(),
                savedModel.getProjectId(),
                command.classId(),
                attribute.getId(),
                attribute.getName(),
                attribute.getType(),
                attribute.getVisibility().name(),
                attribute.getOrderIndex(),
                savedModel.getVersion()
        ));

        UmlAttributeDto dto = new UmlAttributeDto(
                attribute.getId(),
                attribute.getName(),
                attribute.getType(),
                attribute.getVisibility().name(),
                attribute.getOrderIndex()
        );

        AddAttributeResponse response = new AddAttributeResponse(
                command.commandId().toString(),
                command.classId(),
                dto,
                savedModel.getVersion()
        );

        com.umlcase.infrastructure.persistence.entity.ProcessedCommand processedCommand = com.umlcase.infrastructure.persistence.entity.ProcessedCommand.builder()
                .projectId(command.projectId().toString())
                .commandId(command.commandId().toString())
                .commandType("ADD_ATTRIBUTE")
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
