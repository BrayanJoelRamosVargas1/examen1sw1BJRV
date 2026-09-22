package com.umlcase.infrastructure.web.dto;

import com.umlcase.domain.model.RelationshipType;

import java.util.UUID;

public record UpdateRelationshipResponse(
        UUID relationshipId,
        RelationshipType type,
        String sourceMultiplicity,
        String targetMultiplicity,
        long modelVersion
) {}
