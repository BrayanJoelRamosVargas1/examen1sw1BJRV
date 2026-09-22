package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.model.UmlParameter;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.UmlParameterDto;
import com.umlcase.infrastructure.web.dto.UpdateOperationRequest;
import com.umlcase.infrastructure.web.dto.UpdateOperationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        // No usar el profile test para no cargar H2, vamos a usar PostgreSQL real con Testcontainers
        "spring.jpa.show-sql=true"
})
@Testcontainers
// IMPORTANTE: NO USAR @Transactional AQUI para permitir que el HTTP request haga su propio commit
public class UpdateOperationTransactionalIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("umlcase_test")
            .withUsername("umlcase")
            .withPassword("umlcase");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UmlModelRepository repository;

    private UUID projectId;
    private UmlClass cls;
    private UmlOperation operation;
    private long initialVersion;
    private UUID cantidadId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        cls = UmlClass.create("C1");

        operation = UmlOperation.create("procesarPedido", "void", Visibility.PUBLIC);
        cls.addOperation(operation);

        model.addClass(cls);
        initialVersion = repository.save(model).getVersion();
    }

    @Test
    void shouldIncrementVersionOnScalarChangeOnly() {
        var requestBody = new UpdateOperationRequest(
                UUID.randomUUID(), "u1", initialVersion, "calcularPedidoDefinitivo", "void", "PUBLIC",
                List.of()
        );

        String url = "/api/projects/" + projectId + "/classes/" + cls.getId() + "/operations/" + operation.getId();
        ResponseEntity<UpdateOperationResponse> responseEntity = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(requestBody), UpdateOperationResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        UpdateOperationResponse response = responseEntity.getBody();
        long httpModelVersion = response.modelVersion();
        assertEquals(initialVersion + 1, httpModelVersion, "Cambio escalar debe incrementar versión en 1");

        UmlModel dbAfterCommit = repository.findByProjectId(projectId).orElseThrow();
        assertEquals(initialVersion + 1, dbAfterCommit.getVersion(), "DB debe incrementar versión en 1");
    }

    @Test
    void shouldIncrementVersionOnParameterChangeOnly() {
        var requestBody = new UpdateOperationRequest(
                UUID.randomUUID(), "u1", initialVersion, "procesarPedido", "void", "PUBLIC",
                List.of(
                        new UmlParameterDto(null, "p1", "int", 0)
                )
        );

        String url = "/api/projects/" + projectId + "/classes/" + cls.getId() + "/operations/" + operation.getId();
        ResponseEntity<UpdateOperationResponse> responseEntity = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(requestBody), UpdateOperationResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        UpdateOperationResponse response = responseEntity.getBody();
        long httpModelVersion = response.modelVersion();
        assertEquals(initialVersion + 1, httpModelVersion, "Cambio de parámetros debe incrementar versión en 1");

        UmlModel dbAfterCommit = repository.findByProjectId(projectId).orElseThrow();
        assertEquals(initialVersion + 1, dbAfterCommit.getVersion(), "DB debe incrementar versión en 1");
    }

    @Test
    void shouldIncrementVersionOnBothChanges() {
        var requestBody = new UpdateOperationRequest(
                UUID.randomUUID(), "u1", initialVersion, "calcularPedido", "BigDecimal", "PROTECTED",
                List.of(
                        new UmlParameterDto(null, "p1", "int", 0),
                        new UmlParameterDto(null, "p2", "String", 1)
                )
        );

        String url = "/api/projects/" + projectId + "/classes/" + cls.getId() + "/operations/" + operation.getId();
        ResponseEntity<UpdateOperationResponse> responseEntity = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(requestBody), UpdateOperationResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        UpdateOperationResponse response = responseEntity.getBody();
        long httpModelVersion = response.modelVersion();
        assertEquals(initialVersion + 1, httpModelVersion, "Cambio múltiple debe incrementar versión en 1");

        UmlModel dbAfterCommit = repository.findByProjectId(projectId).orElseThrow();
        assertEquals(initialVersion + 1, dbAfterCommit.getVersion(), "DB debe incrementar versión en 1");
    }
}
