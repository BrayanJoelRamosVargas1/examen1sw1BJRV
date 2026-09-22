package com.umlcase.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record UmlOperationDto(
    UUID id,
    String name,
    String returnType,
    String visibility,
    int orderIndex,
    List<UmlParameterDto> parameters
) {}
