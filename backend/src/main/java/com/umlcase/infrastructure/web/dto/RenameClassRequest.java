package com.umlcase.infrastructure.web.dto;

public record RenameClassRequest(
    String commandId,
    String participantId,
    long expectedVersion,
    String newName
) {}
