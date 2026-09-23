package com.umlcase.application.mapper.relational;

import org.springframework.stereotype.Component;

@Component
public class RelationalTypeMapper {
    public String mapType(String umlType) {
        if (umlType == null || umlType.isEmpty()) throw new IllegalArgumentException("UNSUPPORTED_ATTRIBUTE_TYPE: Type cannot be empty");
        return switch (umlType.toLowerCase()) {
            case "string" -> "VARCHAR(255)";
            case "integer", "int" -> "INTEGER";
            case "decimal", "double", "float", "numeric" -> "NUMERIC(19,2)";
            case "boolean", "bool" -> "BOOLEAN";
            case "void" -> throw new IllegalArgumentException("UNSUPPORTED_ATTRIBUTE_TYPE: void is not valid for attributes");
            default -> throw new IllegalArgumentException("UNSUPPORTED_ATTRIBUTE_TYPE: " + umlType);
        };
    }
}
