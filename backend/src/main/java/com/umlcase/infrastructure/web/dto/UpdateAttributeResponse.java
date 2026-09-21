package com.umlcase.infrastructure.web.dto;

import com.umlcase.domain.model.Visibility;
import java.util.UUID;

public record UpdateAttributeResponse(
        UUID commandId,
        UUID classId,
        UmlAttributeDto attribute,
        long modelVersion
) {}
