package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record RemoveRelationshipRequest(
        UUID commandId,
        String participantId,
        long expectedVersion
) {
}
