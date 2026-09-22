package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record RemoveOperationResponse(
        String commandId,
        UUID classId,
        UUID operationId,
        Long modelVersion
) {}
