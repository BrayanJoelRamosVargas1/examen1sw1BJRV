package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record AddRelationshipRequest(
        UUID commandId,
        String participantId,
        long expectedVersion,
        String type,
        UUID sourceClassId,
        UUID targetClassId,
        String sourceMultiplicity,
        String targetMultiplicity
) {}
