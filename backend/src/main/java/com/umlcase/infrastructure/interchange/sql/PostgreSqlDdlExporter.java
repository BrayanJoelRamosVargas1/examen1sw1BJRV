package com.umlcase.infrastructure.interchange.sql;

import com.umlcase.application.port.out.RelationalSchemaExporter;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.domain.relational.RelationalTable;
import com.umlcase.domain.relational.RelationalColumn;
import com.umlcase.domain.relational.RelationalForeignKey;
import com.umlcase.domain.relational.RelationalUniqueConstraint;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostgreSqlDdlExporter implements RelationalSchemaExporter {
    
    @Override
    public byte[] exportToSql(RelationalSchema schema) {
        StringBuilder sql = new StringBuilder();
        
        List<RelationalTable> sortedTables = schema.getTables().stream()
            .sorted(java.util.Comparator.comparing(RelationalTable::getName))
            .collect(Collectors.toList());

        for (RelationalTable table : sortedTables) {
            sql.append("CREATE TABLE ").append(table.getName()).append(" (\n");
            
            String cols = table.getColumns().stream()
                .sorted(java.util.Comparator.comparing(RelationalColumn::name))
                .map(c -> "    " + c.name() + " " + c.type() + (c.isNullable() ? "" : " NOT NULL"))
                .collect(Collectors.joining(",\n"));
            sql.append(cols);
            
            if (table.getPrimaryKey() != null && !table.getPrimaryKey().columns().isEmpty()) {
                sql.append(",\n    PRIMARY KEY (");
                sql.append(table.getPrimaryKey().columns().stream().sorted().collect(Collectors.joining(", ")));
                sql.append(")");
            }
            
            List<RelationalUniqueConstraint> sortedUnique = table.getUniqueConstraints().stream()
                .sorted(java.util.Comparator.comparing(RelationalUniqueConstraint::name))
                .collect(Collectors.toList());
            for (RelationalUniqueConstraint uq : sortedUnique) {
                sql.append(",\n    CONSTRAINT ").append(uq.name()).append(" UNIQUE (");
                sql.append(uq.columns().stream().sorted().collect(Collectors.joining(", "))).append(")");
            }
            
            sql.append("\n);\n\n");
        }
        
        for (RelationalTable table : sortedTables) {
            List<RelationalForeignKey> sortedFks = table.getForeignKeys().stream()
                .sorted(java.util.Comparator.comparing(RelationalForeignKey::name))
                .collect(Collectors.toList());
            for (RelationalForeignKey fk : sortedFks) {
                sql.append("ALTER TABLE ").append(table.getName())
                   .append("\n    ADD CONSTRAINT ").append(fk.name())
                   .append(" FOREIGN KEY (").append(fk.columns().stream().sorted().collect(Collectors.joining(", "))).append(")")
                   .append("\n    REFERENCES ").append(fk.targetTable())
                   .append(" (").append(fk.targetColumns().stream().sorted().collect(Collectors.joining(", "))).append(")");
                if (fk.onDeleteCascade()) {
                    sql.append(" ON DELETE CASCADE");
                }
                sql.append(";\n\n");
            }
        }
        
        return sql.toString().getBytes(StandardCharsets.UTF_8);
    }
}
