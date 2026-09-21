package com.umlcase.infrastructure.web;

import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RenameClassRequest;
import com.umlcase.infrastructure.web.dto.RenameClassResponse;
import org.junit.jupiter.api.DisplayName;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("RenameClassApiIT — Pruebas de integración de la API para renombrar clases")
class RenameClassApiIT {

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

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
    }

    @Autowired
    private UmlModelRepository repository;

    @Test
    @DisplayName("PATCH /api/projects/{projectId}/classes/{classId} con expectedVersion válido renombra la clase y devuelve 200 OK con versión incrementada")
    void renameClass_returns200AndIncrementedVersion() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UmlModel initialModel = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("OriginalName");
        initialModel.addClass(umlClass);
        initialModel = repository.save(initialModel);
        
        Long versionN = initialModel.getVersion();

        String newName = "NewName";
        RenameClassRequest request = new RenameClassRequest(
                UUID.randomUUID().toString(),
                "participant-1",
                versionN,
                newName
        );

        // Act
        ResponseEntity<RenameClassResponse> response = restTemplate.exchange(
                "/api/projects/" + projectId + "/classes/" + umlClass.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                RenameClassResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        RenameClassResponse body = response.getBody();
        assertThat(body.commandId()).isEqualTo(request.commandId());
        assertThat(body.classId()).isEqualTo(umlClass.getId());
        assertThat(body.newName()).isEqualTo(newName);
        assertThat(body.modelVersion()).isGreaterThan(versionN);
    }

    @Test
    @DisplayName("PATCH con expectedVersion obsoleto (stale writer) devuelve 409 CONFLICT")
    void renameClass_staleWriter_returns409() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UmlModel initialModel = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("OriginalName");
        initialModel.addClass(umlClass);
        initialModel = repository.save(initialModel);
        
        Long originalVersion = initialModel.getVersion();

        // Simulate concurrent valid modification (increases DB version)
        initialModel.renameClass(umlClass.getId(), "ConcurrentName");
        repository.save(initialModel);

        // The stale request attempts to send the OLD version
        RenameClassRequest staleRequest = new RenameClassRequest(
                UUID.randomUUID().toString(),
                "participant-stale",
                originalVersion,
                "StaleName"
        );

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/projects/" + projectId + "/classes/" + umlClass.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(staleRequest),
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("MODEL_VERSION_CONFLICT");
    }
}
