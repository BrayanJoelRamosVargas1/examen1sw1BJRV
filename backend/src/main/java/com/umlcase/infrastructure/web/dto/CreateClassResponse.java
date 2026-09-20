package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

/**
 * Respuesta del POST /api/projects/{projectId}/classes
 *
 * Devuelve la versión realmente persistida por JPA (no version+1 manual).
 * El navegador A actualiza su currentVersion con modelVersion.
 * El navegador B la recibe por WebSocket mediante ClassCreatedEvent.modelVersion.
 */
public record CreateClassResponse(
    String commandId,
    UUID classId,
    String className,
    long modelVersion
) {}
