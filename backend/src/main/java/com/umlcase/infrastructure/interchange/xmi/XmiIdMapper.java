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
}
