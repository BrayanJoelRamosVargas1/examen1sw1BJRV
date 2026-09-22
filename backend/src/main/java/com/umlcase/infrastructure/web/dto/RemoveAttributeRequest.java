package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record RemoveAttributeRequest(UUID commandId, String participantId, long expectedVersion) {}
