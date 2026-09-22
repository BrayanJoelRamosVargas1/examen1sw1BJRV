package com.umlcase.application.event;

import java.util.UUID;

public record OperationRemovedEvent(
        String eventType,
        String commandId,
        UUID projectId,
        UUID classId,
        UUID operationId,
        Long modelVersion
) {}
