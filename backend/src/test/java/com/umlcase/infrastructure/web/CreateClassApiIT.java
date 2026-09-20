package com.umlcase.infrastructure.web;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.CreateClassRequest;
import com.umlcase.infrastructure.web.dto.CreateClassResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("CreateClassApiIT — Pruebas de integración de la API para crear clases")
class CreateClassApiIT {

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

    @Test
    @DisplayName("POST /api/projects/{projectId}/classes crea una clase y devuelve 201 Created con la nueva versión del modelo")
    void createClass_returns201AndIncrementedVersion() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UmlModel initialModel = UmlModel.create(projectId);
        repository.save(initialModel);

        String className = "Customer";
        CreateClassRequest request = new CreateClassRequest(
                UUID.randomUUID().toString(),
                "participant-1",
                initialModel.getVersion(),
                className
        );

        // Act
        ResponseEntity<CreateClassResponse> response = restTemplate.postForEntity(
                "/api/projects/" + projectId + "/classes",
                request,
                CreateClassResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        
        CreateClassResponse body = response.getBody();
        assertThat(body.commandId()).isEqualTo(request.commandId());
        assertThat(body.className()).isEqualTo(className);
        assertThat(body.classId()).isNotNull();
        // The new version should be strictly greater than the old version
        assertThat(body.modelVersion()).isGreaterThan(initialModel.getVersion());
    }
}
