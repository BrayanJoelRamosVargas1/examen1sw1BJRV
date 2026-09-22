package com.umlcase.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record UmlClassDto(
    UUID id,
    String name,
    List<UmlAttributeDto> attributes,
    List<UmlOperationDto> operations
) {}
