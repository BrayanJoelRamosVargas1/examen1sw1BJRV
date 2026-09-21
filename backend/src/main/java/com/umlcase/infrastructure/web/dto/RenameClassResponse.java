package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record RenameClassResponse(
    String commandId,
    UUID classId,
    String newName,
    long modelVersion
) {}
