package com.umlcase.application.mapper.relational;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SqlNamingStrategy {
    private final Set<String> usedTableNames = new HashSet<>();

    private final Set<String> usedColumnNames = new HashSet<>();

    public void reset() {
        usedTableNames.clear();
        usedColumnNames.clear();
    }

    public void resetColumns() {
        usedColumnNames.clear();
    }

    public String toTableName(String umlName) {
        String name = toSnakeCase(umlName);
        if (name.isEmpty()) name = "unnamed_table";
        if (usedTableNames.contains(name)) {
            throw new IllegalArgumentException("Colisión de nombres de tabla detectada: " + name);
        }
        usedTableNames.add(name);
        return name;
    }
    
    public String toColumnName(String umlName) {
        return toColumnName(umlName, false);
    }

    public String toColumnName(String umlName, boolean checkCollision) {
        String name = toSnakeCase(umlName);
        if (name.isEmpty()) name = "unnamed_column";
        if (checkCollision) {
            if (usedColumnNames.contains(name)) {
                throw new IllegalArgumentException("Colisión de nombres de columna detectada: " + name);
            }
            usedColumnNames.add(name);
        }
        return name;
    }

    public String toSnakeCase(String str) {
        if (str == null) return "";
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }
}
