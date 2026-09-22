package com.umlcase.application.event;

import java.util.UUID;

public record AttributeRemovedEvent(
        UUID commandId,
        UUID projectId,
        UUID classId,
        UUID attributeId,
        long modelVersion
) {
    public String getEventType() {
        return "ATTRIBUTE_REMOVED";
    }
}
