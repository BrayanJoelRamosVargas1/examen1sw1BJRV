package com.umlcase.application.command;

import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.Visibility;

import java.util.UUID;

/**
 * APLICACIÓN — Contratos de comandos UML.
 *
 * [ADR-004] Command Pattern para modificaciones UML.
 *
 * [CORRECCIÓN — Auditoría Fase 0]
 * Movido de domain/command → application/command.
 * Motivo: los comandos son contratos de la capa de APLICACIÓN, no del dominio.
 * El dominio UML (UmlModel, UmlClass, etc.) no sabe que existen comandos.
 * Los comandos son la interfaz entre el exterior (API/voz/imagen/XMI) y
 * los CommandHandlers de la capa de aplicación.
 *
 * Separación correcta:
 *   EXTERNO → UmlCommand (aplicación) → CommandHandler → UmlModel (dominio)
 *
 * Usamos sealed interface de Java 17 para exhaustive pattern matching.
 * Si se añade un nuevo tipo de comando, el compilador fuerza a manejarlo.
 *
 * Pregunta oral esperada:
 *  - ¿Por qué los comandos no están en el dominio?
 *    → El dominio UML no conoce la intención del usuario. Un UmlModel no
 *      recibe "comandos"; recibe llamadas directas a sus métodos de dominio.
 *      El comando es un DTO de intención que vive en la aplicación.
 *  - ¿Por qué sealed interface? → Para exhaustive pattern matching; con Java 21
 *    (nuestro objetivo) podemos usar un switch(command) y el compilador
 *    garantiza que todos los casos son manejados.
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
        UmlCommand.UpdateRelationship,
        UmlCommand.MoveNode,
        UpdateAttributeCommand {

    /** Id único de este comando. */
    UUID commandId();

    /** Id del proyecto UML al que pertenece este comando. */
    UUID projectId();

    /** Id del participante/usuario que emitió el comando. */
    String participantId();

    /** Versión esperada del modelo sobre la que aplica (concurrencia optimista). */
    long expectedVersion();

    // ─── Comandos de Clase ────────────────────────────────────────────────────

    record CreateClass(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            String className
    ) implements UmlCommand {}

    record RenameClass(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID classId,
            String newName
    ) implements UmlCommand {}

    record DeleteClass(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID classId
    ) implements UmlCommand {}

    // ─── Comandos de Atributo ─────────────────────────────────────────────────

    record AddAttribute(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID classId,
            String attributeName,
            String attributeType,
            Visibility visibility
    ) implements UmlCommand {}

    record RemoveAttribute(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID classId,
            UUID attributeId
    ) implements UmlCommand {}

    // ─── Comandos de Operación ────────────────────────────────────────────────

    record AddOperation(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID classId,
            String operationName,
            String returnType,
            Visibility visibility
    ) implements UmlCommand {}

    record RemoveOperation(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID classId,
            UUID operationId
    ) implements UmlCommand {}

    // ─── Comandos de Relación ─────────────────────────────────────────────────

    record AddRelationship(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            RelationshipType type,
            UUID sourceClassId,
            UUID targetClassId,
            String sourceMultiplicity,
            String targetMultiplicity
    ) implements UmlCommand {}

    record UpdateRelationship(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID relationshipId,
            RelationshipType type,
            String sourceMultiplicity,
            String targetMultiplicity
    ) implements UmlCommand {}

    record RemoveRelationship(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID relationshipId
    ) implements UmlCommand {}

    // ─── Comandos de Vista Visual (ADR-002) ───────────────────────────────────

    record MoveNode(
            UUID commandId,
            UUID projectId,
            String participantId,
            long expectedVersion,
            UUID elementId,
            double newX,
            double newY
    ) implements UmlCommand {}
}
