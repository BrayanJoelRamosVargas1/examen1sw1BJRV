package com.umlcase.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record UpdateOperationRequest(
    UUID commandId,
    String participantId,
    long expectedVersion,
    String name,
    String returnType,
    String visibility,
    List<UmlParameterDto> parameters
) {}
