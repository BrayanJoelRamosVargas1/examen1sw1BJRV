package com.umlcase.infrastructure.web.dto.relational;

import java.util.List;

public record RelationalForeignKeyResponse(
    String name,
    List<String> columns,
    String targetTable,
    List<String> targetColumns,
    boolean onDeleteCascade
) {}
