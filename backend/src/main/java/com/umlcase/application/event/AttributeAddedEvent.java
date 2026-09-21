package com.umlcase.application.event;

import java.util.UUID;

public record AttributeAddedEvent(
    String commandId,
    UUID projectId,
    UUID classId,
    UUID attributeId,
    String name,
    String type,
    String visibility,
    int orderIndex,
    long modelVersion
) {}
