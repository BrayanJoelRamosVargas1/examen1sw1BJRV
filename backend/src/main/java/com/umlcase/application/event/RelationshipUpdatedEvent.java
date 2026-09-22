package com.umlcase.application.event;

import com.umlcase.domain.model.RelationshipType;

import java.util.UUID;

public record RelationshipUpdatedEvent(
        String eventType,
        UUID commandId,
        UUID projectId,
        UUID relationshipId,
        RelationshipType type,
        String sourceMultiplicity,
        String targetMultiplicity,
        long modelVersion
) {
    public RelationshipUpdatedEvent(
            UUID commandId, UUID projectId, UUID relationshipId,
            RelationshipType type,
            String sourceMultiplicity, String targetMultiplicity,
            long modelVersion) {
        this("RELATIONSHIP_UPDATED", commandId, projectId, relationshipId,
             type, sourceMultiplicity, targetMultiplicity, modelVersion);
    }
}
