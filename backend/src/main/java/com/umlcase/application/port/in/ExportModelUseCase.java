package com.umlcase.application.port.in;

import java.util.UUID;

public interface ExportModelUseCase {
    byte[] exportModel(UUID projectId);
}
