package com.umlcase.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateClassRequest(
    @NotBlank String commandId,
    @NotBlank String participantId,
    @NotNull Long expectedVersion,
    @NotBlank String name
) {}
