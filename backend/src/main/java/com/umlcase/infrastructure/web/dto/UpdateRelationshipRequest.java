package com.umlcase.infrastructure.web.dto;

import com.umlcase.domain.model.RelationshipType;

import java.util.UUID;

public record UpdateRelationshipRequest(
        UUID commandId,
        String participantId,
        long expectedVersion,
        RelationshipType type,
        String sourceMultiplicity,
        String targetMultiplicity
) {}
