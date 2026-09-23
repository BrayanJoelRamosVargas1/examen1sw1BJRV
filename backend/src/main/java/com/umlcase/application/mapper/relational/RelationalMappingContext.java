package com.umlcase.application.mapper.relational;

import java.util.HashSet;
import java.util.Set;

public class RelationalMappingContext {
    private final Set<String> usedTableNames = new HashSet<>();
    private Set<String> usedColumnNames = new HashSet<>();

    public void resetColumns() {
        usedColumnNames = new HashSet<>();
    }

    public String generateTableName(String umlName, SqlNamingStrategy namingStrategy) {
        String name = namingStrategy.toSnakeCase(umlName);
        if (name.isEmpty()) name = "unnamed_table";
        if (!usedTableNames.add(name)) {
            throw new IllegalArgumentException("Colisión de nombres de tabla detectada: " + name);
        }
        return name;
    }

    public String generateColumnName(String umlName, SqlNamingStrategy namingStrategy, boolean checkCollision) {
        String name = namingStrategy.toSnakeCase(umlName);
        if (name.isEmpty()) name = "unnamed_column";
        if (checkCollision) {
            if (!usedColumnNames.add(name)) {
                throw new IllegalArgumentException("Colisión de nombres de columna detectada: " + name);
            }
        }
        return name;
    }
}
