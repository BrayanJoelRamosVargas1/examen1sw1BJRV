package com.umlcase.infrastructure.interchange.sql;

import com.umlcase.application.port.out.RelationalSchemaExporter;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.domain.relational.RelationalTable;
import com.umlcase.domain.relational.RelationalColumn;
import com.umlcase.domain.relational.RelationalForeignKey;
import com.umlcase.domain.relational.RelationalUniqueConstraint;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class PostgreSqlDdlExporter implements RelationalSchemaExporter {
    
    @Override
    public byte[] exportToSql(RelationalSchema schema) {
        StringBuilder sql = new StringBuilder();
        
        for (RelationalTable table : schema.getTables()) {
            sql.append("CREATE TABLE ").append(table.getName()).append(" (\n");
            
            String cols = table.getColumns().stream()
                .map(c -> "    " + c.name() + " " + c.type() + (c.isNullable() ? "" : " NOT NULL"))
                .collect(Collectors.joining(",\n"));
            sql.append(cols);
            
            if (table.getPrimaryKey() != null && !table.getPrimaryKey().columns().isEmpty()) {
                sql.append(",\n    PRIMARY KEY (");
                sql.append(String.join(", ", table.getPrimaryKey().columns()));
                sql.append(")");
            }
            
            for (RelationalUniqueConstraint uq : table.getUniqueConstraints()) {
                sql.append(",\n    CONSTRAINT ").append(uq.name()).append(" UNIQUE (");
                sql.append(String.join(", ", uq.columns())).append(")");
            }
            
            sql.append("\n);\n\n");
        }
        
        for (RelationalTable table : schema.getTables()) {
            for (RelationalForeignKey fk : table.getForeignKeys()) {
                sql.append("ALTER TABLE ").append(table.getName())
                   .append("\n    ADD CONSTRAINT ").append(fk.name())
                   .append(" FOREIGN KEY (").append(String.join(", ", fk.columns())).append(")")
                   .append("\n    REFERENCES ").append(fk.targetTable())
                   .append(" (").append(String.join(", ", fk.targetColumns())).append(")");
                if (fk.onDeleteCascade()) {
                    sql.append(" ON DELETE CASCADE");
                }
                sql.append(";\n\n");
            }
        }
        
        return sql.toString().getBytes(StandardCharsets.UTF_8);
    }
}
