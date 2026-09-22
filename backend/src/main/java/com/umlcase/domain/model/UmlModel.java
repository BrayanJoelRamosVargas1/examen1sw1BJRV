package com.umlcase.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * DOMINIO UML — Modelo UML canónico (semántico).
 *
 * Representa el estado semántico completo de un diagrama de clases UML.
 * Contiene clases y relaciones. Es la fuente de verdad del dominio.
 *
 * [DECISIÓN DE DISEÑO] Esta clase no tiene posición visual (x, y).
 * El layout visual vive en UmlDiagram. Ver ADR-002.
 *
 * [DECISIÓN DE DISEÑO] No tiene anotaciones JPA. Ver ADR-001.
 *
 * Preguntas orales esperadas:
 *  - ¿Por qué UmlModel no tiene posición x,y? → ADR-002, separación semántica/visual.
 *  - ¿Dónde se persiste? → Puerto UmlModelRepository (ADR-001).
 *  - ¿Cómo se modifica? → Solo mediante UmlCommand + CommandHandler (ADR-004).
 */
public final class UmlModel {

    private final UUID id;
    private final UUID projectId;
    private long version;
    private final List<UmlClass> classes;
    private final List<UmlRelationship> relationships;

    public UmlModel(UUID id, UUID projectId, long version) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.projectId = Objects.requireNonNull(projectId, "projectId no puede ser null");
        this.version = version;
        this.classes = new ArrayList<>();
        this.relationships = new ArrayList<>();
    }

    public static UmlModel create(UUID projectId) {
        return new UmlModel(UUID.randomUUID(), projectId, 0L);
    }

    // ─── Operaciones de dominio ───────────────────────────────────────────────

    /**
     * Agrega una clase al modelo.
     * No se permiten dos clases con el mismo nombre dentro del modelo.
     */
    public void addClass(UmlClass umlClass) {
        Objects.requireNonNull(umlClass, "umlClass no puede ser null");
        boolean duplicate = classes.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(umlClass.getName()));
        if (duplicate) {
            throw new IllegalArgumentException(
                "Ya existe una clase con nombre '" + umlClass.getName() + "' en el modelo");
        }
        classes.add(umlClass);
    }

    /**
     * Renombra una clase existente en el modelo.
     * Invariantes:
     * 1. La clase debe existir.
     * 2. No puede haber otra clase con el mismo nombre nuevo.
     */
    public void renameClass(UUID classId, String newName) {
        Objects.requireNonNull(classId, "classId no puede ser null");
        Objects.requireNonNull(newName, "newName no puede ser null");

        UmlClass classToRename = findClassById(classId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la clase con id " + classId));

        boolean duplicate = classes.stream()
            .anyMatch(c -> !c.getId().equals(classId) && c.getName().equalsIgnoreCase(newName));

        if (duplicate) {
            throw new IllegalArgumentException("Ya existe otra clase con el nombre '" + newName + "' en el modelo");
        }

        classToRename.rename(newName);
    }

    /**
     * Elimina una clase y todas sus relaciones.
     * Regla de dominio: no pueden quedar relaciones huérfanas.
     */
    public void removeClass(UUID classId) {
        classes.removeIf(c -> c.getId().equals(classId));
        // Eliminar relaciones que involucran a la clase eliminada
        relationships.removeIf(r ->
                r.getSourceClassId().equals(classId) ||
                r.getTargetClassId().equals(classId));
    }

    /**
     * Agrega un atributo a una clase existente.
     * Delega en UmlClass para la validación de duplicados y asigna el orderIndex al final.
     */
    public UmlAttribute addAttribute(UUID classId, String name, String type, Visibility visibility) {
        UmlClass targetClass = findClassById(classId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la clase con id " + classId));
        
        int nextOrderIndex = targetClass.getAttributes().stream()
                .mapToInt(UmlAttribute::getOrderIndex)
                .max()
                .orElse(-1) + 1;
                
        UmlAttribute newAttribute = new UmlAttribute(UUID.randomUUID(), name, type, visibility, nextOrderIndex);
        targetClass.addAttribute(newAttribute);
        return newAttribute;
    }

    /**
     * Actualiza un atributo existente en una clase.
     * Delega en UmlClass para la validación de duplicados (case-insensitive).
     */
    public UmlAttribute updateAttribute(UUID classId, UUID attributeId, String name, String type, Visibility visibility) {
        UmlClass targetClass = findClassById(classId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la clase con id " + classId));
        targetClass.updateAttribute(attributeId, name, type, visibility);
        return targetClass.getAttributes().stream()
                .filter(a -> a.getId().equals(attributeId))
                .findFirst()
                .orElseThrow();
    }

    /** Elimina un atributo existente sin modificar el orden de los demás. */
    public void removeAttribute(UUID classId, UUID attributeId) {
        UmlClass targetClass = findClassById(classId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la clase con id " + classId));
        targetClass.removeAttribute(attributeId);
    }

    /**
     * Agrega una relación entre dos clases.
     * Valida que las clases referenciadas existan en el modelo.
     */
    public void addRelationship(UmlRelationship relationship) {
        Objects.requireNonNull(relationship, "relationship no puede ser null");
        boolean sourceExists = findClassById(relationship.getSourceClassId()).isPresent();
        boolean targetExists = findClassById(relationship.getTargetClassId()).isPresent();
        if (!sourceExists || !targetExists) {
            throw new IllegalArgumentException(
                "Las clases fuente y destino deben existir en el modelo antes de crear una relación");
        }
        relationships.add(relationship);
    }

    /** Elimina una relación por id. */
    public void removeRelationship(UUID relationshipId) {
        relationships.removeIf(r -> r.getId().equals(relationshipId));
    }

    /** Busca una clase por id. */
    public Optional<UmlClass> findClassById(UUID id) {
        return classes.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    /** Busca una clase por nombre (case-insensitive). */
    public Optional<UmlClass> findClassByName(String name) {
        return classes.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()        { return id; }
    public UUID getProjectId() { return projectId; }
    public long getVersion()   { return version; }
    public void setVersion(long version) { this.version = version; }

    public List<UmlClass> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    public List<UmlRelationship> getRelationships() {
        return Collections.unmodifiableList(relationships);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlModel other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "UmlModel{id=" + id
               + ", classes=" + classes.size()
               + ", relationships=" + relationships.size() + "}";
    }
}
