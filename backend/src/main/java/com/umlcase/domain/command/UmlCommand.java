package com.umlcase.domain.command;

import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.Visibility;

import java.util.UUID;

/**
 * DOMINIO — Contratos de comandos UML.
 *
 * [ADR-004] Command Pattern para modificaciones UML.
 *
 * Toda modificación al UmlModel se expresa como un UmlCommand.
 * El CommandHandler (capa de aplicación) procesa el comando
 * independientemente de si provino de la UI, voz, imagen o XMI.
 *
 * Usamos sealed interface de Java 17 para exhaustive pattern matching.
 * Si se añade un nuevo tipo de comando, el compilador fuerza a manejarlo.
 *
 * Pregunta oral esperada:
 *  - ¿Por qué sealed? → Para que el compilador garantice que todos los
 *    comandos son manejados en el switch (exhaustive matching).
 *  - ¿Por qué Records? → Inmutabilidad, concisión y claridad de intención.
 */
public sealed interface UmlCommand permits
        UmlCommand.CreateClass,
        UmlCommand.RenameClass,
        UmlCommand.DeleteClass,
        UmlCommand.AddAttribute,
        UmlCommand.RemoveAttribute,
        UmlCommand.AddOperation,
        UmlCommand.RemoveOperation,
        UmlCommand.AddRelationship,
        UmlCommand.RemoveRelationship,
        UmlCommand.MoveNode {

    /** El id del proyecto UML al que pertenece este comando. */
    UUID projectId();

    // ─── Comandos de Clase ────────────────────────────────────────────────────

    /**
     * Crear una nueva clase UML con el nombre dado.
     * La posición inicial en el canvas se indica con x, y.
     */
    record CreateClass(
            UUID projectId,
            String className,
            double x,
            double y
    ) implements UmlCommand {}

    /**
     * Renombrar una clase existente.
     */
    record RenameClass(
            UUID projectId,
            UUID classId,
            String newName
    ) implements UmlCommand {}

    /**
     * Eliminar una clase y todas sus relaciones asociadas.
     */
    record DeleteClass(
            UUID projectId,
            UUID classId
    ) implements UmlCommand {}

    // ─── Comandos de Atributo ─────────────────────────────────────────────────

    /**
     * Agregar un atributo a una clase existente.
     */
    record AddAttribute(
            UUID projectId,
            UUID classId,
            String attributeName,
            String attributeType,
            Visibility visibility
    ) implements UmlCommand {}

    /**
     * Eliminar un atributo de una clase.
     */
    record RemoveAttribute(
            UUID projectId,
            UUID classId,
            UUID attributeId
    ) implements UmlCommand {}

    // ─── Comandos de Operación ────────────────────────────────────────────────

    /**
     * Agregar una operación (método) a una clase existente.
     */
    record AddOperation(
            UUID projectId,
            UUID classId,
            String operationName,
            String returnType,
            Visibility visibility
    ) implements UmlCommand {}

    /**
     * Eliminar una operación de una clase.
     */
    record RemoveOperation(
            UUID projectId,
            UUID classId,
            UUID operationId
    ) implements UmlCommand {}

    // ─── Comandos de Relación ─────────────────────────────────────────────────

    /**
     * Agregar una relación entre dos clases.
     */
    record AddRelationship(
            UUID projectId,
            RelationshipType type,
            UUID sourceClassId,
            UUID targetClassId,
            String sourceMultiplicity,
            String targetMultiplicity
    ) implements UmlCommand {}

    /**
     * Eliminar una relación.
     */
    record RemoveRelationship(
            UUID projectId,
            UUID relationshipId
    ) implements UmlCommand {}

    // ─── Comandos de Vista Visual ─────────────────────────────────────────────

    /**
     * Mover un nodo en el canvas.
     * [ADR-002] Solo afecta UmlNodeView, no UmlClass.
     */
    record MoveNode(
            UUID projectId,
            UUID elementId,
            double newX,
            double newY
    ) implements UmlCommand {}
}
