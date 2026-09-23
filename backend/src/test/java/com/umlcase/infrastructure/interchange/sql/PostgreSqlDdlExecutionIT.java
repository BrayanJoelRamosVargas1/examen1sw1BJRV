package com.umlcase.infrastructure.interchange.sql;

import com.umlcase.domain.model.*;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.application.mapper.relational.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class PostgreSqlDdlExecutionIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1-alpine")
            .withDatabaseName("umlcase_execution_test")
            .withUsername("umlcase")
            .withPassword("umlcase");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void executeGeneratedDdl_onPostgres_succeeds() {
        SqlNamingStrategy naming = new SqlNamingStrategy();
        UmlToRelationalMapper mapper = new UmlToRelationalMapper(naming, new RelationalTypeMapper(), new RelationshipRelationalMapper(naming));
        PostgreSqlDdlExporter exporter = new PostgreSqlDdlExporter();

        UmlModel model = UmlModel.create(UUID.randomUUID());

        UmlClass persona = new UmlClass(UUID.randomUUID(), "Persona");
        persona.addAttribute(new UmlAttribute(UUID.randomUUID(), "nombre", "String", Visibility.PUBLIC, 0));
        
        UmlClass cliente = new UmlClass(UUID.randomUUID(), "Cliente");
        cliente.addAttribute(new UmlAttribute(UUID.randomUUID(), "descuento", "Decimal", Visibility.PUBLIC, 0));
        
        UmlClass pedido = new UmlClass(UUID.randomUUID(), "Pedido");
        pedido.addAttribute(new UmlAttribute(UUID.randomUUID(), "total", "Decimal", Visibility.PUBLIC, 0));
        
        UmlClass linea = new UmlClass(UUID.randomUUID(), "LineaPedido");
        linea.addAttribute(new UmlAttribute(UUID.randomUUID(), "cantidad", "Integer", Visibility.PUBLIC, 0));
        
        UmlClass producto = new UmlClass(UUID.randomUUID(), "Producto");
        producto.addAttribute(new UmlAttribute(UUID.randomUUID(), "precio", "Decimal", Visibility.PUBLIC, 0));

        model.addClass(persona);
        model.addClass(cliente);
        model.addClass(pedido);
        model.addClass(linea);
        model.addClass(producto);

        // Generalization
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.GENERALIZATION, cliente.getId(), persona.getId(), null, null));
        
        // 1:N
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.ASSOCIATION, cliente.getId(), pedido.getId(), "1", "0..*"));
        
        // Composition 1:N
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.COMPOSITION, pedido.getId(), linea.getId(), "1", "1..*"));
        
        // N:M
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.ASSOCIATION, linea.getId(), producto.getId(), "0..*", "0..*"));

        RelationalSchema schema = mapper.mapToRelational(model);
        String ddl = new String(exporter.exportToSql(schema));

        // Execute DDL
        jdbcTemplate.execute(ddl);

        // Query information schema
        List<Map<String, Object>> tables = jdbcTemplate.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema='public'");
        List<String> tableNames = tables.stream().map(m -> (String) m.get("table_name")).toList();
        
        assertThat(tableNames).contains("persona", "cliente", "pedido", "linea_pedido", "producto", "linea_pedido_producto");
        
        // Check columns of 'cliente'
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='cliente'");
        List<String> colNames = columns.stream().map(m -> (String) m.get("column_name")).toList();
        assertThat(colNames).contains("id", "descuento");
        
        // Check constraints
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList("SELECT constraint_name, constraint_type FROM information_schema.table_constraints WHERE table_schema='public' AND table_name='cliente'");
        assertThat(constraints.stream().anyMatch(c -> "PRIMARY KEY".equals(c.get("constraint_type")))).isTrue();
        assertThat(constraints.stream().anyMatch(c -> "FOREIGN KEY".equals(c.get("constraint_type")))).isTrue();
    }
}
