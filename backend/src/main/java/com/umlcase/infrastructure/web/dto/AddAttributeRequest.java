package com.umlcase.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddAttributeRequest(
    @NotNull(message = "expectedVersion es obligatorio")
    Long expectedVersion,
    
    @NotBlank(message = "participantId es obligatorio")
    String participantId,
    
    @NotBlank(message = "attributeName es obligatorio")
    String attributeName,
    
    @NotBlank(message = "attributeType es obligatorio")
    String attributeType,
    
    @NotNull(message = "visibility es obligatorio")
    String visibility,
    
    UUID commandId
) {}
