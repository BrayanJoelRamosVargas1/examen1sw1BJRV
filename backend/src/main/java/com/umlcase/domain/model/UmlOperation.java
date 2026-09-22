package com.umlcase.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DOMINIO UML — Operación (método) de una clase UML.
 *
 * Representa una operation en UML 2.5.
 * Ejemplo: + calcularTotal() : Double
 *
 * [DECISIÓN DE DISEÑO] Sin anotaciones de infraestructura. Ver ADR-001.
 */
public final class UmlOperation {

    private final UUID id;
    private String name;
    private String returnType;
    private Visibility visibility;
    private final List<UmlParameter> parameters;
    private int orderIndex;

    public UmlOperation(UUID id, String name, String returnType,
                        Visibility visibility, int orderIndex) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.name = validateName(name);
        this.returnType = returnType == null ? "void" : returnType.trim();
        this.visibility = Objects.requireNonNull(visibility);
        this.parameters = new ArrayList<>();
        this.orderIndex = orderIndex;
    }

    public static UmlOperation create(String name, String returnType, Visibility visibility) {
        return new UmlOperation(UUID.randomUUID(), name, returnType, visibility, 0);
    }

    public static UmlOperation create(String name) {
        return new UmlOperation(UUID.randomUUID(), name, "void", Visibility.PUBLIC, 0);
    }

    // ─── Mutaciones ───────────────────────────────────────────────────────────

    public void rename(String newName)           { this.name = validateName(newName); }
    public void changeReturnType(String rt)      { this.returnType = rt == null ? "void" : rt.trim(); }
    public void changeVisibility(Visibility v)   { this.visibility = Objects.requireNonNull(v); }
    public void setOrderIndex(int i)             { this.orderIndex = i; }

    public void addParameter(UmlParameter param) {
        Objects.requireNonNull(param);
        boolean duplicate = parameters.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(param.getName()));
        if (duplicate) {
            throw new IllegalArgumentException("Ya existe un parámetro con nombre '" + param.getName() + "'");
        }
        param.setOrderIndex(parameters.size());
        parameters.add(param);
    }

    public void removeParameter(UUID paramId) {
        parameters.removeIf(p -> p.getId().equals(paramId));
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()           { return id; }
    public String getName()       { return name; }
    public String getReturnType() { return returnType; }
    public Visibility getVisibility() { return visibility; }
    public int getOrderIndex()    { return orderIndex; }
    public List<UmlParameter> getParameters() { return Collections.unmodifiableList(parameters); }

    /** Representación UML: "+ calcularTotal(monto : Double) : Double" */
    public String toUmlString() {
        String params = parameters.stream()
                .map(UmlParameter::toUmlString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return visibility.toUmlSymbol() + " " + name + "(" + params + ") : " + returnType;
    }

    // ─── Validación ───────────────────────────────────────────────────────────

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la operación no puede ser vacío");
        }
        return name.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlOperation other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() { return "UmlOperation{" + toUmlString() + "}"; }
}
