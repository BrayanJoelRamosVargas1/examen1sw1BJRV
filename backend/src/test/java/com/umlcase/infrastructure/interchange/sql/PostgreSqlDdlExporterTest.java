package com.umlcase.infrastructure.interchange.sql;

import com.umlcase.domain.relational.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlDdlExporterTest {

    private final PostgreSqlDdlExporter exporter = new PostgreSqlDdlExporter();

    @Test
    void exportToSql_generatesTablesAndForeignKeys() {
        RelationalSchema schema = new RelationalSchema();
        
        RelationalTable t1 = new RelationalTable("cliente");
        t1.addColumn(new RelationalColumn("id", "UUID", false));
        t1.addColumn(new RelationalColumn("nombre", "VARCHAR(255)", true));
        t1.setPrimaryKey(new RelationalPrimaryKey(List.of("id")));
        
        RelationalTable t2 = new RelationalTable("pedido");
        t2.addColumn(new RelationalColumn("id", "UUID", false));
        t2.addColumn(new RelationalColumn("cliente_id", "UUID", false));
        t2.setPrimaryKey(new RelationalPrimaryKey(List.of("id")));
        t2.addForeignKey(new RelationalForeignKey("fk_pedido_cliente_id", List.of("cliente_id"), "cliente", List.of("id"), true));
        
        schema.addTable(t1);
        schema.addTable(t2);

        String sql = new String(exporter.exportToSql(schema), StandardCharsets.UTF_8);
        
        assertThat(sql).contains("CREATE TABLE cliente (");
        assertThat(sql).contains("id UUID NOT NULL");
        assertThat(sql).contains("PRIMARY KEY (id)");
        
        assertThat(sql).contains("CREATE TABLE pedido (");
        assertThat(sql).contains("cliente_id UUID NOT NULL");
        
        assertThat(sql).contains("ALTER TABLE pedido");
        assertThat(sql).contains("ADD CONSTRAINT fk_pedido_cliente_id FOREIGN KEY (cliente_id)");
        assertThat(sql).contains("REFERENCES cliente (id) ON DELETE CASCADE");
    }
}
