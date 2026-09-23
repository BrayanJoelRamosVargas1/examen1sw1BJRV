package com.umlcase.infrastructure.web.dto.relational;

import java.util.List;

public record RelationalUniqueConstraintResponse(String name, List<String> columns) {}
