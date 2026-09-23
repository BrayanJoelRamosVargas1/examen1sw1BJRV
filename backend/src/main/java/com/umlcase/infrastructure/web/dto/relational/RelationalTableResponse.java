package com.umlcase.infrastructure.web.dto.relational;

import java.util.List;

public record RelationalTableResponse(
    String name,
    RelationalPrimaryKeyResponse primaryKey,
    List<RelationalColumnResponse> columns,
    List<RelationalForeignKeyResponse> foreignKeys,
    List<RelationalUniqueConstraintResponse> uniqueConstraints
) {}
