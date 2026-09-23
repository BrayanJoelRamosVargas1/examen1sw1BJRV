package com.umlcase.infrastructure.web.dto;

public record ImportModelResponse(
        String commandId,
        int modelVersion,
        int classCount,
        int relationshipCount
) {}
