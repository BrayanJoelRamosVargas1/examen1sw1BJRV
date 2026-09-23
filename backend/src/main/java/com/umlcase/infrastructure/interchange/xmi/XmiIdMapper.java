package com.umlcase.infrastructure.interchange.xmi;

import java.util.UUID;

/**
 * Mapeador determinista de IDs para exportación XMI.
 * [PENDIENTE VALIDACIÓN EA REAL]
 */
public class XmiIdMapper {

    private static final String EA_PREFIX = "EAID_";

    /**
     * Convierte un UUID interno en un xmi:id estable.
     * Retiene los guiones del UUID.
     */
    public String toXmiId(UUID internalId) {
        if (internalId == null) {
            return null;
        }
        return EA_PREFIX + internalId.toString();
    }

    /**
     * Convierte un xmi:id en un UUID interno de manera determinista.
     * Si el xmi:id tiene el formato EAID_ + UUID, extrae el UUID.
     * Si no, genera un UUID basado en el hash del string (UUID v3).
     */
    public UUID toUuid(String xmiId) {
        if (xmiId == null || xmiId.isEmpty()) {
            return UUID.randomUUID();
        }
        
        if (xmiId.startsWith(EA_PREFIX)) {
            String suffix = xmiId.substring(EA_PREFIX.length());
            try {
                return UUID.fromString(suffix);
            } catch (IllegalArgumentException e) {
                // No es un UUID válido, generar uno determinista
            }
        }
        
        return UUID.nameUUIDFromBytes(xmiId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
