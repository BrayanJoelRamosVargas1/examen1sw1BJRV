package com.umlcase.infrastructure.web.dto;

public record RemoveOperationRequest(
        String commandId,
        String participantId,
        Long expectedVersion
) {}
