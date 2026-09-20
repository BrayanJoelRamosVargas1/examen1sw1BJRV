package com.umlcase.application.event;

import java.util.UUID;

public record ClassCreatedEvent(
    String commandId,
    UUID projectId,
    UUID classId,
    String className,
    long modelVersion
) {}
