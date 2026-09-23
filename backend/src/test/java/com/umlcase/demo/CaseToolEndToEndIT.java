package com.umlcase.demo;

import com.umlcase.application.mapper.java.UmlToJavaModelMapper;
import com.umlcase.application.mapper.relational.UmlToRelationalMapper;
import com.umlcase.application.port.in.GeneratedProject;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.infrastructure.interchange.spring.ZipSpringBootProjectExporter;
import com.umlcase.infrastructure.interchange.sql.PostgreSqlDdlExporter;
import com.umlcase.infrastructure.interchange.xmi.XmiExportAdapter;
import com.umlcase.infrastructure.interchange.xmi.XmiImportAdapter;
import com.umlcase.infrastructure.interchange.xmi.UmlModelSemanticAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class CaseToolEndToEndIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1-alpine")
        .withDatabaseName("umlcase_demo_test")
        .withUsername("umlcase")
        .withPassword("umlcase");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired private UmlToRelationalMapper relationalMapper;
    @Autowired private UmlToJavaModelMapper javaMapper;
    @Autowired private ZipSpringBootProjectExporter zipExporter;
    @Autowired private com.umlcase.application.port.out.RelationalSchemaExporter sqlExporter;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void canonicalModelSurvivesInterchangeRelationalAndGenerationChain() throws Exception {
        UmlModel original = DemoUmlFixture.create();
        assertThat(original.getClasses()).hasSize(6);
        assertThat(original.getRelationships()).hasSize(5);

        XmiExportAdapter xmiExporter = new XmiExportAdapter();
        XmiImportAdapter xmiImporter = new XmiImportAdapter();
        UmlModel imported = xmiImporter.importModel(xmiExporter.export(original));
        UmlModelSemanticAssert.assertModelsSemanticallyEquivalent(original, imported);

        RelationalSchema schema = relationalMapper.mapToRelational(original);
        String ddl = new String(new PostgreSqlDdlExporter().exportToSql(schema));
        jdbcTemplate.execute(ddl);
        assertThat(jdbcTemplate.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema='public'"))
            .isNotEmpty();

        GeneratedProject generated = zipExporter.exportToZip(javaMapper.mapToJavaModel(original), sqlExporter.exportToSql(schema));
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(generated.content()))) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                entries.add(entry.getName());
                entry = zip.getNextEntry();
            }
        }
        assertThat(entries).anyMatch(name -> name.endsWith("pom.xml"));
        assertThat(entries).anyMatch(name -> name.endsWith("Cliente.java"));
        assertThat(entries).anyMatch(name -> name.endsWith("schema.sql"));
    }
}
