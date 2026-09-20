package com.umlcase.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * DOMINIO UML — Atributo de una clase UML.
 *
 * Representa una propiedad (attribute/property en UML 2.5).
 * Ejemplo: - nombre : String
 *
 * [DECISIÓN DE DISEÑO] Sin anotaciones de infraestructura. Ver ADR-001.
 */
public final class UmlAttribute {

    private final UUID id;
    private String name;
    private String type;
    private Visibility visibility;
    private int orderIndex;

    public UmlAttribute(UUID id, String name, String type, Visibility visibility, int orderIndex) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.name = validateName(name);
        this.type = validateType(type);
        this.visibility = Objects.requireNonNull(visibility, "visibility no puede ser null");
        this.orderIndex = orderIndex;
    }

    public static UmlAttribute create(String name, String type, Visibility visibility) {
        return new UmlAttribute(UUID.randomUUID(), name, type, visibility, 0);
    }

    public static UmlAttribute create(String name, String type) {
        return new UmlAttribute(UUID.randomUUID(), name, type, Visibility.PRIVATE, 0);
    }

    // ─── Mutaciones permitidas ────────────────────────────────────────────────

    public void rename(String newName) { this.name = validateName(newName); }
    public void changeType(String newType) { this.type = validateType(newType); }
    public void changeVisibility(Visibility v) {
        this.visibility = Objects.requireNonNull(v, "visibility no puede ser null");
    }
    public void setOrderIndex(int index) { this.orderIndex = index; }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()           { return id; }
    public String getName()       { return name; }
    public String getType()       { return type; }
    public Visibility getVisibility() { return visibility; }
    public int getOrderIndex()    { return orderIndex; }

    /** Representación UML textual: "- nombre : String" */
    public String toUmlString() {
        return visibility.toUmlSymbol() + " " + name + " : " + type;
    }

    // ─── Validaciones ─────────────────────────────────────────────────────────

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del atributo no puede ser vacío");
        }
        return name.trim();
    }

    private static String validateType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("El tipo del atributo no puede ser vacío");
        }
        return type.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlAttribute other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "UmlAttribute{" + toUmlString() + "}";
    }
}
