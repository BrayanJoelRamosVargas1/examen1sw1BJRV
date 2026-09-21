package com.umlcase.infrastructure.web.dto;

import java.util.UUID;

public record AddAttributeResponse(
    String commandId,
    UUID classId,
    UmlAttributeDto attribute,
    long version
) {}
