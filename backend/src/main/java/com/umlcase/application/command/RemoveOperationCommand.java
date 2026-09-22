package com.umlcase.application.command;

import java.util.UUID;

public record RemoveOperationCommand(
        String commandId,
        String participantId,
        UUID projectId,
        UUID classId,
        UUID operationId,
        Long expectedVersion
) {}
