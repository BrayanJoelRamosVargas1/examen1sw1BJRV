package com.umlcase.domain.relational;

import java.util.List;

public record RelationalUniqueConstraint(String name, List<String> columns) {}
