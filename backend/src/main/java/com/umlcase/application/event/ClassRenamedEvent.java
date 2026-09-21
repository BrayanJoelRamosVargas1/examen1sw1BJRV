package com.umlcase.application.event;

import java.util.UUID;

public record ClassRenamedEvent(
    String commandId,
    UUID projectId,
    UUID classId,
    String newName,
    long modelVersion
) {}
