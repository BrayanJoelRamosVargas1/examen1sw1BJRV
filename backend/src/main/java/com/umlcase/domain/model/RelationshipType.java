package com.umlcase.domain.model;

/**
 * DOMINIO UML — Tipos de relación entre clases UML.
 *
 * [DECISIÓN DE DISEÑO — ALCANCE] Solo se implementan los tipos que
 * corresponden a diagramas de clases para modelado conceptual.
 * No se implementa UML completo. Ver BASELINE_REQUISITOS.md §3.
 *
 * Referencia: UML 2.5 specification, OMG.
 */
public enum RelationshipType {

    /**
     * Asociación: relación estructural entre dos clases.
     * Notación UML: línea sólida (con o sin navegabilidad).
     */
    ASSOCIATION,

    /**
     * Generalización (herencia): una clase es subclase de otra.
     * Notación UML: línea sólida con punta de flecha triangular vacía.
     */
    GENERALIZATION,

    /**
     * Agregación: relación "todo-parte" débil.
     * El "parte" puede existir sin el "todo".
     * Notación UML: línea con rombo vacío en el extremo del "todo".
     */
    AGGREGATION,

    /**
     * Composición: relación "todo-parte" fuerte.
     * El "parte" no puede existir sin el "todo".
     * Notación UML: línea con rombo relleno en el extremo del "todo".
     */
    COMPOSITION,

    /**
     * Dependencia: cambio en una clase puede afectar a otra.
     * Notación UML: línea discontinua con flecha abierta.
     * [DECISIÓN DE DISEÑO — ALCANCE] Incluido si el tiempo permite.
     */
    DEPENDENCY
}
