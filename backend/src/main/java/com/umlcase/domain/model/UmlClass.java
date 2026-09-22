package com.umlcase.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DOMINIO UML — Clase UML.
 *
 * Representa una clase en el diagrama de clases UML.
 * Contiene la semántica pura: nombre, atributos y operaciones.
 *
 * [DECISIÓN DE DISEÑO] Esta clase NO tiene anotaciones JPA, Spring ni
 * dependencias de infraestructura. El dominio desconoce PostgreSQL.
 * Ver ADR-001 y ADR-002.
 *
 * Preguntas orales esperadas:
 *  - ¿Por qué no tiene @Entity? → Separación dominio/infraestructura (ADR-001).
 *  - ¿Dónde se persiste? → A través del puerto UmlModelRepository (ADR-001).
 *  * ¿Qué significa final? → final impide heredar de UmlClass; la creación controlada se realiza mediante create(...)
 */
public final class UmlClass {

    private final UUID id;
    private String name;
    private final List<UmlAttribute> attributes;
    private final List<UmlOperation> operations;

    /**
     * Constructor principal. Requiere id y name válidos.
     * Los atributos y operaciones se agregan mediante los métodos de dominio.
     */
    public UmlClass(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.name = validateName(name);
        this.attributes = new ArrayList<>();
        this.operations = new ArrayList<>();
    }

    /** Factory method para crear con UUID generado automáticamente. */
    public static UmlClass create(String name) {
        return new UmlClass(UUID.randomUUID(), name);
    }

    // ─── Reglas de dominio ────────────────────────────────────────────────────

    /**
     * Renombra la clase. Aplica validación de dominio.
     * Una clase UML no puede tener nombre vacío ni nulo.
     */
    public void rename(String newName) {
        this.name = validateName(newName);
    }

    /**
     * Agrega un atributo a la clase.
     * No permite atributos con nombre duplicado dentro de la misma clase.
     */
    public void addAttribute(UmlAttribute attribute) {
        Objects.requireNonNull(attribute, "atributo no puede ser null");
        boolean duplicate = attributes.stream()
                .anyMatch(a -> a.getName().equalsIgnoreCase(attribute.getName()));
        if (duplicate) {
            throw new IllegalArgumentException(
                "Ya existe un atributo con nombre '" + attribute.getName()
                + "' en la clase '" + this.name + "'");
        }
        attributes.add(attribute);
    }

    /**
     * Actualiza un atributo existente.
     * Rechaza la actualización si el nuevo nombre colisiona con otro atributo (case-insensitive).
     */
    public void updateAttribute(UUID attributeId, String newName, String newType, Visibility newVisibility) {
        UmlAttribute target = attributes.stream()
                .filter(a -> a.getId().equals(attributeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El atributo con id " + attributeId + " no existe en la clase " + this.name));
        
        boolean duplicate = attributes.stream()
                .anyMatch(a -> !a.getId().equals(attributeId) && a.getName().equalsIgnoreCase(newName));
        if (duplicate) {
            throw new IllegalArgumentException(
                "Ya existe otro atributo con nombre '" + newName + "' en la clase '" + this.name + "'");
        }
        
        target.rename(newName);
        target.changeType(newType);
        target.changeVisibility(newVisibility);
    }

    /**
     * Agrega una operación a la clase.
     * UML permite overloading, por lo que se acepta mismo nombre si los
     * parámetros son diferentes. En Fase 0 simplemente los agrega.
     */
    public void addOperation(UmlOperation operation) {
        Objects.requireNonNull(operation, "operación no puede ser null");
        operations.add(operation);
    }

    /** Elimina un atributo por id. */
    public void removeAttribute(UUID attributeId) {
        Objects.requireNonNull(attributeId, "attributeId no puede ser null");
        boolean removed = attributes.removeIf(a -> a.getId().equals(attributeId));
        if (!removed) {
            throw new IllegalArgumentException("El atributo con id " + attributeId + " no existe en la clase " + this.name);
        }
    }

    /** Elimina una operación por id. */
    public void removeOperation(UUID operationId) {
        operations.removeIf(o -> o.getId().equals(operationId));
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }

    public String getName() { return name; }

    /** Retorna vista inmutable de atributos. */
    public List<UmlAttribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    /** Retorna vista inmutable de operaciones. */
    public List<UmlOperation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    // ─── Validación de dominio ────────────────────────────────────────────────

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de una clase UML no puede ser vacío");
        }
        return name.trim();
    }

    // ─── equals/hashCode por identidad de id ─────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlClass other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "UmlClass{id=" + id + ", name='" + name + "'"
               + ", attributes=" + attributes.size()
               + ", operations=" + operations.size() + "}";
    }
}
