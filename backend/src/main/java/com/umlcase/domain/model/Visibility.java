package com.umlcase.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * DOMINIO UML — Visibilidad de un elemento UML.
 *
 * UML 2.5 define cuatro tipos de visibilidad para atributos y operaciones.
 * Usamos un enum de dominio puro (sin dependencias de frameworks).
 */
public enum Visibility {
    PUBLIC,    // +
    PRIVATE,   // -
    PROTECTED, // #
    PACKAGE;   // ~

    /** Retorna el símbolo UML estándar. */
    public String toUmlSymbol() {
        return switch (this) {
            case PUBLIC    -> "+";
            case PRIVATE   -> "-";
            case PROTECTED -> "#";
            case PACKAGE   -> "~";
        };
    }
}
