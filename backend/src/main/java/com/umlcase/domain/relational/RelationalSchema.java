package com.umlcase.domain.relational;

import java.util.ArrayList;
import java.util.List;

public class RelationalSchema {
    private final List<RelationalTable> tables = new ArrayList<>();

    public void addTable(RelationalTable table) {
        this.tables.add(table);
    }

    public List<RelationalTable> getTables() {
        return List.copyOf(tables);
    }
}
