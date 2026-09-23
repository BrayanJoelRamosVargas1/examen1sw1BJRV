package com.umlcase.infrastructure.web;

import com.umlcase.application.mapper.java.UmlToJavaModelMapper;
import com.umlcase.application.mapper.relational.UmlToRelationalMapper;
import com.umlcase.application.port.in.GeneratedProject;
import com.umlcase.domain.generation.java.GeneratedJavaModel;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.infrastructure.interchange.spring.ZipSpringBootProjectExporter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class GeneratedBackendCrudIT {

    @Autowired
    private ZipSpringBootProjectExporter exporter;

    @Autowired
    private UmlToJavaModelMapper javaMapper;

    @Autowired
    private UmlToRelationalMapper relationalMapper;

    @Autowired
    private com.umlcase.application.port.out.RelationalSchemaExporter sqlExporter;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void generatedProject_crudOperationsWork() throws Exception {
        // 1. Fixture UML
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 0L);

        var cliente = UmlClass.create("Cliente");
        model.addClass(cliente);
        model.addAttribute(cliente.getId(), "nombre", "String", com.umlcase.domain.model.Visibility.PUBLIC);
        model.addAttribute(cliente.getId(), "activo", "Boolean", com.umlcase.domain.model.Visibility.PUBLIC);

        var pedido = UmlClass.create("Pedido");
        model.addClass(pedido);
        model.addAttribute(pedido.getId(), "total", "Decimal", com.umlcase.domain.model.Visibility.PUBLIC);

        model.addRelationship(new UmlRelationship(
            UUID.randomUUID(), com.umlcase.domain.model.RelationshipType.ASSOCIATION,
            cliente.getId(), pedido.getId(), "1", "0..*"
        ));

        // 2. Mapeo y generación
        RelationalSchema schema = relationalMapper.mapToRelational(model);
        GeneratedJavaModel javaModel = javaMapper.mapToJavaModel(model);
        byte[] sql = sqlExporter.exportToSql(schema);
        GeneratedProject project = exporter.exportToZip(javaModel, sql);

        // 3. Extraer a TempDir
        Path tempDir = Files.createTempDirectory("crud-backend");
        File projectDir = new File(tempDir.toFile(), "generated-backend");
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(project.content()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(tempDir.toFile(), entry.getName());
                if (entry.getName().endsWith("/")) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        zis.transferTo(fos);
                    }
                }
            }
        }

        // 4. Compilar proyecto con Java 21 en Docker (maven package)
        String absolutePath = projectDir.getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "--rm", 
                "-v", absolutePath + ":/usr/src/mymaven", 
                "-w", "/usr/src/mymaven", 
                "maven:3.9-eclipse-temurin-21", 
                "mvn", "clean", "package", "-DskipTests"
        );
        pb.directory(projectDir);
        File buildLog = new File(projectDir, "build.log");
        pb.redirectOutput(buildLog);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            String output = Files.readString(buildLog.toPath());
            System.err.println("BUILD FAILED:\n" + output);
        }
        assertThat(exitCode).isEqualTo(0).withFailMessage("Compilation for generated project failed");

        // 5. Iniciar la app en un contenedor y conectar al Postgres host
        // Testcontainers for host access mapping:
        org.testcontainers.Testcontainers.exposeHostPorts(postgres.getMappedPort(5432));
        
        String dbUrl = "jdbc:postgresql://host.testcontainers.internal:" + postgres.getMappedPort(5432) + "/" + postgres.getDatabaseName();
        
        File jarFile = new File(projectDir, "target/generated-backend-0.0.1-SNAPSHOT.jar");
        
        try (GenericContainer<?> appContainer = new GenericContainer<>("eclipse-temurin:21-jre")) {
            appContainer
                .withExposedPorts(8080)
                .withCopyFileToContainer(MountableFile.forHostPath(jarFile.getAbsolutePath()), "/app.jar")
                .withCommand("java", "-jar", "/app.jar")
                .withEnv("DB_URL", dbUrl)
                .withEnv("DB_USERNAME", postgres.getUsername())
                .withEnv("DB_PASSWORD", postgres.getPassword())
                .waitingFor(Wait.forHttp("/api/cliente").forStatusCode(200));

            appContainer.start();

            String baseUrl = "http://" + appContainer.getHost() + ":" + appContainer.getMappedPort(8080);
            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 6. Test CRUD - POST Cliente
            String createJson = "{\"nombre\": \"Brayan\", \"activo\": true}";
            HttpEntity<String> createRequest = new HttpEntity<>(createJson, headers);
            ResponseEntity<Map> createResponse = rest.postForEntity(baseUrl + "/api/cliente", createRequest, Map.class);
            
            assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
            Map<?, ?> createdCliente = createResponse.getBody();
            assertThat(createdCliente).isNotNull();
            String clienteId = createdCliente.get("id").toString();
            assertThat(clienteId).isNotNull();
            assertThat(createdCliente.get("nombre")).isEqualTo("Brayan");
            assertThat(createdCliente.get("activo")).isEqualTo(true);

            // GET Cliente
            ResponseEntity<Map> getResponse = rest.getForEntity(baseUrl + "/api/cliente/" + clienteId, Map.class);
            assertThat(getResponse.getStatusCode().value()).isEqualTo(200);
            assertThat(getResponse.getBody().get("nombre")).isEqualTo("Brayan");

            // PUT Cliente
            String updateJson = "{\"nombre\": \"Brayan Actualizado\", \"activo\": false}";
            HttpEntity<String> updateRequest = new HttpEntity<>(updateJson, headers);
            rest.exchange(baseUrl + "/api/cliente/" + clienteId, HttpMethod.PUT, updateRequest, Map.class);

            ResponseEntity<Map> getUpdatedResponse = rest.getForEntity(baseUrl + "/api/cliente/" + clienteId, Map.class);
            assertThat(getUpdatedResponse.getBody().get("nombre")).isEqualTo("Brayan Actualizado");
            assertThat(getUpdatedResponse.getBody().get("activo")).isEqualTo(false);

            // DELETE Cliente
            rest.delete(baseUrl + "/api/cliente/" + clienteId);

            // GET eliminado -> 404
            HttpClientErrorException ex = assertThrows(HttpClientErrorException.NotFound.class, () -> {
                rest.getForEntity(baseUrl + "/api/cliente/" + clienteId, Map.class);
            });
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
            
            // 7. TEST FK via JDBC directly (to avoid complex Service/DTO generator changes for relations)
            // Re-create Cliente via HTTP to use as valid FK reference
            ResponseEntity<Map> c2 = rest.postForEntity(baseUrl + "/api/cliente", createRequest, Map.class);
            String validClienteId = c2.getBody().get("id").toString();

            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                
                // check flyway schema history exists
                try (java.sql.ResultSet rs = conn.createStatement().executeQuery("SELECT installed_rank FROM flyway_schema_history")) {
                    assertThat(rs.next()).isTrue();
                }

                // Insert Pedido with valid Cliente FK
                String insertSql = "INSERT INTO pedido (id, total, cliente_id) VALUES (?, ?, ?)";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setDouble(2, 200.0);
                    ps.setObject(3, UUID.fromString(validClienteId));
                    int rows = ps.executeUpdate();
                    assertThat(rows).isEqualTo(1);
                }

                // Insert Pedido with INVALID Cliente FK should fail
                String insertFailSql = "INSERT INTO pedido (id, total, cliente_id) VALUES (?, ?, ?)";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(insertFailSql)) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setDouble(2, 300.0);
                    ps.setObject(3, UUID.randomUUID()); // Fake UUID
                    
                    org.postgresql.util.PSQLException psqlEx = assertThrows(org.postgresql.util.PSQLException.class, () -> {
                        ps.executeUpdate();
                    });
                    assertThat(psqlEx.getMessage()).contains("violates foreign key constraint");
                }
            }
        }
    }
}
