package com.umlcase.application.event;

import com.umlcase.domain.model.RelationshipType;

import java.util.UUID;

public record RelationshipAddedEvent(
        String eventType,
        UUID commandId,
        UUID projectId,
        UUID relationshipId,
        UUID sourceClassId,
        UUID targetClassId,
        RelationshipType relationshipType,
        String sourceMultiplicity,
        String targetMultiplicity,
        long modelVersion
) {
    public RelationshipAddedEvent(
            UUID commandId, UUID projectId, UUID relationshipId,
            UUID sourceClassId, UUID targetClassId,
            RelationshipType relationshipType,
            String sourceMultiplicity, String targetMultiplicity,
            long modelVersion) {
        this("RELATIONSHIP_ADDED", commandId, projectId, relationshipId,
             sourceClassId, targetClassId, relationshipType,
             sourceMultiplicity, targetMultiplicity, modelVersion);
    }
}
