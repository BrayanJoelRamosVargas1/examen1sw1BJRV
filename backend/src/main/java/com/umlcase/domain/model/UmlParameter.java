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
    private int orderIndex;

    public UmlParameter(UUID id, String name, String type, int orderIndex) {
        this.id = Objects.requireNonNull(id);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del parámetro no puede ser vacío");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("El tipo del parámetro no puede ser vacío");
        }
        this.name = name.trim();
        this.type = type.trim();
        this.orderIndex = orderIndex;
    }

    public static UmlParameter create(String name, String type) {
        return new UmlParameter(UUID.randomUUID(), name, type, 0);
    }

    public UUID getId()   { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

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
