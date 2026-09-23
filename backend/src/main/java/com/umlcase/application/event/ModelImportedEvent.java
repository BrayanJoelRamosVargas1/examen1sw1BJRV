package com.umlcase.application.event;

import java.time.Instant;
import java.util.UUID;

public record ModelImportedEvent(
        UUID projectId,
        String participantId,
        String commandId,
        int modelVersion,
        int classCount,
        int relationshipCount,
        Instant timestamp
) {

    public ModelImportedEvent(UUID projectId, String participantId, String commandId, int modelVersion, int classCount, int relationshipCount) {
        this(projectId, participantId, commandId, modelVersion, classCount, relationshipCount, Instant.now());
    }
}
