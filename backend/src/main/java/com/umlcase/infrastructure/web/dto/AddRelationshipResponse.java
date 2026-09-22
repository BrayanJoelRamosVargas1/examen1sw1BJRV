package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record AddRelationshipResponse(
        UUID commandId,
        UUID relationshipId,
        long modelVersion,
        UmlRelationshipDto relationship
) {}
