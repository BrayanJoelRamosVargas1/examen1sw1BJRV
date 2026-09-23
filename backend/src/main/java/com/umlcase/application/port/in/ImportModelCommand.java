package com.umlcase.application.port.in;

import java.util.UUID;

public record ImportModelCommand(
        UUID projectId,
        String participantId,
        String commandId,
        int expectedVersion,
        byte[] xmiContent
) {}
