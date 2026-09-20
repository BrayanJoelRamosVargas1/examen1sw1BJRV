package com.umlcase.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * DOMINIO UML — Relación entre dos clases UML.
 *
 * Una relación conecta una clase fuente (source) con una clase destino (target).
 * Tiene un tipo (ASSOCIATION, GENERALIZATION, etc.) y multiplicidades opcionales.
 *
 * Ejemplos de multiplicidad: "1", "*", "0..1", "1..*", "2..5"
 *
 * [DECISIÓN DE DISEÑO] Sin anotaciones JPA. Ver ADR-001.
 *
 * Preguntas orales esperadas:
 *  - ¿Qué diferencia hay entre Aggregation y Composition?
 *    → En Composition el "parte" no existe sin el "todo" (cardinalidad del ciclo de vida).
 *  - ¿Qué es Generalization en UML? → La relación de herencia entre clases.
 */
public final class UmlRelationship {

    private final UUID id;
    private final RelationshipType type;
    private final UUID sourceClassId;
    private final UUID targetClassId;
    private String sourceMultiplicity;
    private String targetMultiplicity;

    public UmlRelationship(UUID id, RelationshipType type,
                           UUID sourceClassId, UUID targetClassId,
                           String sourceMultiplicity, String targetMultiplicity) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.type = Objects.requireNonNull(type, "type no puede ser null");
        this.sourceClassId = Objects.requireNonNull(sourceClassId, "sourceClassId no puede ser null");
        this.targetClassId = Objects.requireNonNull(targetClassId, "targetClassId no puede ser null");
        this.sourceMultiplicity = sourceMultiplicity;
        this.targetMultiplicity = targetMultiplicity;
    }

    public static UmlRelationship create(RelationshipType type,
                                         UUID sourceClassId,
                                         UUID targetClassId) {
        return new UmlRelationship(UUID.randomUUID(), type, sourceClassId, targetClassId, null, null);
    }

    public static UmlRelationship create(RelationshipType type,
                                         UUID sourceClassId,
                                         UUID targetClassId,
                                         String sourceMultiplicity,
                                         String targetMultiplicity) {
        return new UmlRelationship(UUID.randomUUID(), type, sourceClassId, targetClassId,
                                   sourceMultiplicity, targetMultiplicity);
    }

    // ─── Mutaciones ───────────────────────────────────────────────────────────

    public void setSourceMultiplicity(String m) { this.sourceMultiplicity = m; }
    public void setTargetMultiplicity(String m) { this.targetMultiplicity = m; }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()                     { return id; }
    public RelationshipType getType()       { return type; }
    public UUID getSourceClassId()          { return sourceClassId; }
    public UUID getTargetClassId()          { return targetClassId; }
    public String getSourceMultiplicity()   { return sourceMultiplicity; }
    public String getTargetMultiplicity()   { return targetMultiplicity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlRelationship other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "UmlRelationship{id=" + id
               + ", type=" + type
               + ", source=" + sourceClassId
               + ", target=" + targetClassId + "}";
    }
}
