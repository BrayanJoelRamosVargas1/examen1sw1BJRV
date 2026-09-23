package com.umlcase.application.port.in;

import java.util.UUID;

public interface GenerateBackendUseCase {

    GeneratedProject generate(UUID projectId);
}
