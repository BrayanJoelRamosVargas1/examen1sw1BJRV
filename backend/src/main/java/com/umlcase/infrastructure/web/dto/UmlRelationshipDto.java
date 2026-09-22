package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record UmlRelationshipDto(
    UUID id,
    String type,
    UUID sourceClassId,
    UUID targetClassId,
    String sourceMultiplicity,
    String targetMultiplicity
) {}
