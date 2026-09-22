package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record RemoveRelationshipResponse(
        UUID commandId,
        UUID relationshipId,
        long modelVersion
) {
}
