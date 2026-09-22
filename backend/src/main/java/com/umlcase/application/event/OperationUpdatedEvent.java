package com.umlcase.application.event;

import com.umlcase.domain.model.Visibility;
import java.util.List;
import java.util.UUID;

public record OperationUpdatedEvent(
    String eventType,
    UUID commandId,
    UUID projectId,
    UUID classId,
    UUID operationId,
    String name,
    String returnType,
    Visibility visibility,
    int orderIndex,
    List<ParameterData> parameters,
    long modelVersion
) {
    public record ParameterData(UUID id, String name, String type, int orderIndex) {}

    public OperationUpdatedEvent {
        if (!"OPERATION_UPDATED".equals(eventType)) {
            throw new IllegalArgumentException("eventType debe ser OPERATION_UPDATED");
        }
    }
}
