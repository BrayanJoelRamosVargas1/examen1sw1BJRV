package com.umlcase.application.event;

import java.util.UUID;

public record RelationshipRemovedEvent(
        String eventType,
        UUID commandId,
        UUID projectId,
        UUID relationshipId,
        long modelVersion
) {
    public RelationshipRemovedEvent(
            UUID commandId,
            UUID projectId,
            UUID relationshipId,
            long modelVersion) {
        this("RELATIONSHIP_REMOVED", commandId, projectId, relationshipId, modelVersion);
    }
}
