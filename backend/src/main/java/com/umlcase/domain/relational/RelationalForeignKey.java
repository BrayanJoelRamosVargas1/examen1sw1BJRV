package com.umlcase.domain.relational;

import java.util.List;

public record RelationalForeignKey(
    String name,
    List<String> columns,
    String targetTable,
    List<String> targetColumns,
    boolean onDeleteCascade
) {}
