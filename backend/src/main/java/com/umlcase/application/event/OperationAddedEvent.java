package com.umlcase.application.event;

import com.umlcase.domain.model.Visibility;
import java.util.List;
import java.util.UUID;

public record OperationAddedEvent(
        UUID commandId,
        UUID projectId,
        UUID classId,
        UUID operationId,
        String name,
        String returnType,
        Visibility visibility,
        int orderIndex,
        List<ParameterInfo> parameters,
        long modelVersion
) {
    public String getEventType() {
        return "OPERATION_ADDED";
    }

    public record ParameterInfo(
            UUID id,
            String name,
            String type,
            int orderIndex
    ) {}
}
