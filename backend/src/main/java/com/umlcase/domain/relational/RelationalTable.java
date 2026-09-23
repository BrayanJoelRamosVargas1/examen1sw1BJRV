package com.umlcase.domain.relational;

import java.util.ArrayList;
import java.util.List;

public class RelationalTable {
    private final String name;
    private RelationalPrimaryKey primaryKey;
    private final List<RelationalColumn> columns = new ArrayList<>();
    private final List<RelationalForeignKey> foreignKeys = new ArrayList<>();
    private final List<RelationalUniqueConstraint> uniqueConstraints = new ArrayList<>();

    public RelationalTable(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public RelationalPrimaryKey getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(RelationalPrimaryKey primaryKey) { this.primaryKey = primaryKey; }
    public List<RelationalColumn> getColumns() { return List.copyOf(columns); }
    public void addColumn(RelationalColumn column) { this.columns.add(column); }
    public List<RelationalForeignKey> getForeignKeys() { return List.copyOf(foreignKeys); }
    public void addForeignKey(RelationalForeignKey fk) { this.foreignKeys.add(fk); }
    public List<RelationalUniqueConstraint> getUniqueConstraints() { return List.copyOf(uniqueConstraints); }
    public void addUniqueConstraint(RelationalUniqueConstraint uc) { this.uniqueConstraints.add(uc); }
}
