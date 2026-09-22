package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record RemoveAttributeResponse(UUID commandId, UUID classId, UUID attributeId, long modelVersion) {}
