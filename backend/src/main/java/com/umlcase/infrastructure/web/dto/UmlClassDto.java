package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record UmlClassDto(
    UUID id,
    String name
) {}
