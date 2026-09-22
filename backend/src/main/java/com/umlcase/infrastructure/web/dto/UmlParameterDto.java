package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record UmlParameterDto(
    UUID id,
    String name,
    String type,
    int orderIndex
) {}
