package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record UmlAttributeDto(
    UUID id,
    String name,
    String type,
    String visibility,
    int orderIndex
) {}
