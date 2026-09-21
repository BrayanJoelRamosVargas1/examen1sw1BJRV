package com.umlcase.infrastructure.web.dto;

import com.umlcase.domain.model.Visibility;
import java.util.UUID;

public record UpdateAttributeResponse(
        UUID commandId,
        UUID classId,
        UUID attributeId,
        String name,
        String type,
        Visibility visibility,
        int orderIndex,
        long modelVersion
) {}
