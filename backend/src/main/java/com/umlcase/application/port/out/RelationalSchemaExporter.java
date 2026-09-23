package com.umlcase.application.port.out;

import com.umlcase.domain.relational.RelationalSchema;

public interface RelationalSchemaExporter {
    byte[] exportToSql(RelationalSchema schema);
}
