package com.umlcase.application.command;

import com.umlcase.domain.model.Visibility;
import java.util.List;
import java.util.UUID;

public record UpdateOperationCommand(
    UUID commandId,
    UUID projectId,
    String participantId,
    long expectedVersion,
    UUID classId,
    UUID operationId,
    String name,
    String returnType,
    Visibility visibility,
    List<ParameterData> parameters
) {
    public record ParameterData(UUID id, String name, String type) {}
}
