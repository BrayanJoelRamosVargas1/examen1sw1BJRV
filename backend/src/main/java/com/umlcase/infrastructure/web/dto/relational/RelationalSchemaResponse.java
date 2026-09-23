package com.umlcase.infrastructure.web.dto.relational;

import java.util.List;

public record RelationalSchemaResponse(List<RelationalTableResponse> tables) {}
