package com.umlcase.application.command;

import com.umlcase.domain.model.Visibility;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateAttributeCommand(
        UUID commandId,
        UUID projectId,
        String participantId,
        long expectedVersion,
        UUID classId,
        UUID attributeId,
        String name,
        String type,
        Visibility visibility
) implements UmlCommand {}
