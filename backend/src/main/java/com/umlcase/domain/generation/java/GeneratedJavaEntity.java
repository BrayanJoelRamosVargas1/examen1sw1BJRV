package com.umlcase.domain.generation.java;

import java.util.List;
import java.util.UUID;

public record GeneratedJavaEntity(
    UUID umlClassId,
    String className,
    String tableName,
    String primaryKeyName,
    String primaryKeyType,
    String parentClassName, // null if no parent
    boolean isJoinedInheritanceRoot,
    List<GeneratedJavaField> fields,
    List<GeneratedJavaRelationship> relationships
) {}
