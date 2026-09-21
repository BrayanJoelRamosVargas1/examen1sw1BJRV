package com.umlcase.application.event;

import com.umlcase.domain.model.Visibility;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AttributeUpdatedEvent(
        UUID commandId,
        UUID projectId,
        UUID classId,
        UUID attributeId,
        String name,
        String type,
        Visibility visibility,
        int orderIndex,
        long modelVersion
) {
    public String getEventType() {
        return "ATTRIBUTE_UPDATED";
    }
}
