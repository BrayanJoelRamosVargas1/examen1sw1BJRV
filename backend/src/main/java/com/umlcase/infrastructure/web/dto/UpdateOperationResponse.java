package com.umlcase.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record UpdateOperationResponse(
    UUID commandId,
    UUID classId,
    UUID operationId,
    String name,
    String returnType,
    String visibility,
    int orderIndex,
    List<UmlParameterDto> parameters,
    long modelVersion
) {}
