package com.umlcase.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record UmlModelDto(
    UUID projectId,
    long version,
    List<UmlClassDto> classes
) {}
