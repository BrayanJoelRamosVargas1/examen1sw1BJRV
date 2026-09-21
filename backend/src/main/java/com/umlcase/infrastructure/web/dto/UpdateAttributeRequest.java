package com.umlcase.infrastructure.web.dto;

import com.umlcase.domain.model.Visibility;
import java.util.UUID;

public record UpdateAttributeRequest(
        UUID commandId,
        String participantId,
        long expectedVersion,
        String name,
        String type,
        Visibility visibility
) {}
