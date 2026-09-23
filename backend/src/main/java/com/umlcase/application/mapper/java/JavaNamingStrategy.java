package com.umlcase.application.mapper.java;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JavaNamingStrategy {

    private static final Set<String> RESERVED_WORDS = Set.of(
        "abstract", "continue", "for", "new", "switch",
        "assert", "default", "goto", "package", "synchronized",
        "boolean", "do", "if", "private", "this",
        "break", "double", "implements", "protected", "throw",
        "byte", "else", "import", "public", "throws",
        "case", "enum", "instanceof", "return", "transient",
        "catch", "extends", "int", "short", "try",
        "char", "final", "interface", "static", "void",
        "class", "finally", "long", "strictfp", "volatile",
        "const", "float", "native", "super", "while"
    );

    public String toClassName(String umlName) {
        if (umlName == null || umlName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la clase UML no puede estar vacío");
        }
        String normalized = normalize(umlName);
        normalized = Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
        if (RESERVED_WORDS.contains(normalized.toLowerCase())) {
            normalized = normalized + "Entity";
        }
        if (!isValidJavaIdentifier(normalized)) {
            throw new IllegalArgumentException("No se pudo generar un nombre de clase Java válido para: " + umlName);
        }
        return normalized;
    }

    public String toFieldName(String umlName) {
        if (umlName == null || umlName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del atributo UML no puede estar vacío");
        }
        String normalized = normalize(umlName);
        normalized = Character.toLowerCase(normalized.charAt(0)) + normalized.substring(1);
        if (RESERVED_WORDS.contains(normalized)) {
            normalized = "_" + normalized;
        }
        if (!isValidJavaIdentifier(normalized)) {
            throw new IllegalArgumentException("No se pudo generar un nombre de campo Java válido para: " + umlName);
        }
        return normalized;
    }

    private String normalize(String name) {
        StringBuilder normalized = new StringBuilder();
        boolean capitalizeNext = false;

        for (char character : name.trim().toCharArray()) {
            if (character == ' ' || character == '-' || character == '.') {
                capitalizeNext = true;
                continue;
            }
            if (!Character.isLetterOrDigit(character) && character != '_' && character != '$') {
                continue;
            }
            if (capitalizeNext && normalized.length() > 0) {
                normalized.append(Character.toUpperCase(character));
            } else {
                normalized.append(character);
            }
            capitalizeNext = false;
        }

        String s = normalized.toString();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("El nombre '" + name + "' no contiene caracteres válidos.");
        }
        if (Character.isDigit(s.charAt(0))) {
            s = "_" + s;
        }
        return s;
    }

    private boolean isValidJavaIdentifier(String s) {
        if (s.isEmpty()) return false;
        if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
