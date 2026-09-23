package com.umlcase.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import com.umlcase.application.event.ClassRenamedEvent;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RenameClassRequest;
import com.umlcase.infrastructure.web.dto.RenameClassResponse;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.mockito.Mockito;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("ConcurrentModelMutationIT — Conflicto concurrente real A/B (Fase 4.1)")
class ConcurrentModelMutationIT {

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
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
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

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        Mockito.reset(messagingTemplate);
    }

    @Test
    @DisplayName("Escenario concurrente: A y B envían mutación con same-version. First wins, second gets 409 and no event is emitted.")
    void concurrentMutation_firstWins_secondGets409_noExtraEvents() {
        // Arrange: Estado inicial
        UUID projectId = UUID.randomUUID();
        UmlModel initialModel = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("ClienteVIP1");
        initialModel.addClass(umlClass);
        initialModel = repository.save(initialModel);
        
        Long versionN = initialModel.getVersion();

        // Construir Command A (expectedVersion = N)
        RenameClassRequest requestA = new RenameClassRequest(
                UUID.randomUUID().toString(),
                "browser-A",
                versionN,
                "ClienteA"
        );

        // Construir Command B (expectedVersion = N)
        RenameClassRequest requestB = new RenameClassRequest(
                UUID.randomUUID().toString(),
                "browser-B",
                versionN,
                "ClienteB"
        );

        // Act 1: Ejecutar A (llega primero)
        ResponseEntity<RenameClassResponse> responseA = restTemplate.exchange(
                "/api/projects/" + projectId + "/classes/" + umlClass.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(requestA),
                RenameClassResponse.class
        );

        // Act 2: Ejecutar B (llega tarde con el expectedVersion viejo)
        ResponseEntity<String> responseB = restTemplate.exchange(
                "/api/projects/" + projectId + "/classes/" + umlClass.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(requestB),
                String.class
        );

        // Assert: A tuvo éxito
        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseA.getBody()).isNotNull();
        Long versionNPlus1 = responseA.getBody().modelVersion();
        assertThat(versionNPlus1).isGreaterThan(versionN);

        // Assert: B fue rechazado con 409
        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        String errorCode = JsonPath.parse(responseB.getBody()).read("$.code", String.class);
        assertThat(errorCode).isEqualTo("MODEL_VERSION_CONFLICT");

        // Assert: DB state and Version 
        UmlModel finalModel = repository.findById(initialModel.getId()).orElseThrow();
        assertThat(finalModel.getVersion()).isEqualTo(versionNPlus1); // Solo 1 incremento de versión (N -> N+1)
        assertThat(finalModel.getClasses().iterator().next().getName()).isEqualTo("ClienteA"); // Mutación A persistió

        // Assert: Events 
        Mockito.verify(messagingTemplate, Mockito.times(1))
                .convertAndSend(Mockito.eq("/topic/projects/" + projectId), Mockito.any(ClassRenamedEvent.class));
    }
}
