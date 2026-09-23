package com.umlcase.application.ai;

public record InterpretedUmlCommand(
    String type,
    String className,
    String newClassName,
    String attributeName,
    String attributeType,
    String operationName,
    String relationshipType,
    String sourceClass,
    String targetClass,
    String sourceMultiplicity,
    String targetMultiplicity
) {}
