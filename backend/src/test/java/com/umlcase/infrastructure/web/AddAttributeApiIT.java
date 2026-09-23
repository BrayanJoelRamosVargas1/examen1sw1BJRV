package com.umlcase.infrastructure.web;

import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.AddAttributeRequest;
import com.umlcase.infrastructure.web.dto.AddAttributeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AddAttributeApiIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UmlModelRepository repository;

    private UUID projectId;
    private UUID classId;
    private long initialVersion;

    @BeforeEach
    @Transactional
    void setUp() {
        // Configuramos la base de datos con un proyecto, modelo y una clase
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        
        UmlClass umlClass = UmlClass.create("ClaseDePrueba");
        classId = umlClass.getId();
        model.addClass(umlClass);

        UmlModel savedModel = repository.save(model);
        initialVersion = savedModel.getVersion();
    }

    @Test
    void addAttribute_success_returns201() {
        // Arrange
        String url = "/api/projects/" + projectId + "/classes/" + classId + "/attributes";
        
        AddAttributeRequest request = new AddAttributeRequest(
                initialVersion,
                "participant-1",
                "newAttr",
                "int",
                "PRIVATE",
                UUID.randomUUID()
        );

        // Act
        ResponseEntity<AddAttributeResponse> response = restTemplate.postForEntity(url, request, AddAttributeResponse.class);

        // Assert HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Assert Body
        AddAttributeResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.attribute()).isNotNull();
        assertThat(body.attribute().name()).isEqualTo("newAttr");
        assertThat(body.attribute().type()).isEqualTo("int");
        assertThat(body.attribute().visibility()).isEqualTo("PRIVATE");
        assertThat(body.attribute().orderIndex()).isEqualTo(0);
        assertThat(body.modelVersion()).isEqualTo(initialVersion + 1);
        
        // Assert Persistencia (concurrencia N -> N+1)
        UmlModel storedModel = repository.findByProjectId(projectId).orElseThrow();
        assertThat(storedModel.getVersion()).isGreaterThan(initialVersion);
        
        UmlClass storedClass = storedModel.findClassById(classId).orElseThrow();
        assertThat(storedClass.getAttributes()).hasSize(1);
        assertThat(storedClass.getAttributes().get(0).getName()).isEqualTo("newAttr");
    }

    @Test
    void addAttribute_staleWriter_returns409() {
        // Arrange
        String url = "/api/projects/" + projectId + "/classes/" + classId + "/attributes";

        // Simulamos que otro usuario (user-B) hace un cambio primero, incrementando la versión real
        UmlModel model = repository.findByProjectId(projectId).orElseThrow();
        model.addAttribute(classId, "nombre", "String", com.umlcase.domain.model.Visibility.PRIVATE);
        model.setVersion(initialVersion); // aseguro persistencia correcta
        repository.save(model);
        // Ahora la base de datos está en version = initialVersion + 1 o superior

        // user-A intenta añadir su atributo enviando la versión vieja que él conocía (initialVersion)
        AddAttributeRequest staleRequest = new AddAttributeRequest(
                initialVersion,
                "user-A",
                "edad",
                "int",
                "PRIVATE",
                UUID.randomUUID()
        );

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(url, staleRequest, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("MODEL_VERSION_CONFLICT");
    }
}
