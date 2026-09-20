package com.umlcase.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * DOMINIO UML — Parámetro de una operación UML.
 * Ejemplo: monto : Double
 */
public final class UmlParameter {

    private final UUID id;
    private String name;
    private String type;

    public UmlParameter(UUID id, String name, String type) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name).trim();
        this.type = Objects.requireNonNull(type).trim();
    }

    public static UmlParameter create(String name, String type) {
        return new UmlParameter(UUID.randomUUID(), name, type);
    }

    public UUID getId()   { return id; }
    public String getName() { return name; }
    public String getType() { return type; }

    /** Representación UML: "monto : Double" */
    public String toUmlString() { return name + " : " + type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlParameter other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
