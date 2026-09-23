package com.umlcase.application.port.in;

import com.umlcase.domain.relational.RelationalSchema;
import java.util.UUID;

public interface GenerateRelationalSchemaUseCase {
    RelationalSchema generateSchema(UUID projectId);
}
