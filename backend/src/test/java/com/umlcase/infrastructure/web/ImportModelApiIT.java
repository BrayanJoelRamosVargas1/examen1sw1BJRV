package com.umlcase.infrastructure.web;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("ImportModelApiIT — Reemplazo XMI transaccional")
class ImportModelApiIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1-alpine")
            .withDatabaseName("umlcase_test")
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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UmlModelRepository repository;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        repository.save(model);
    }

    @AfterEach
    void tearDown() {
        repository.findByProjectId(projectId).ifPresent(m -> repository.deleteById(m.getId()));
    }

    @Test
    @DisplayName("Importa XMI y reemplaza modelo transaccionalmente N -> N+1")
    void importXmiReplacesModel() {
        UmlModel initialModel = repository.findByProjectId(projectId).orElseThrow();
        long initialVersion = initialModel.getVersion();

        String xmi = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xmi:XMI xmlns:xmi="http://schema.omg.org/spec/XMI/2.1" xmlns:uml="http://schema.omg.org/spec/UML/2.1" xmi:version="2.1">
                  <uml:Model name="UmlModelExport" xmi:id="EAID_UmlModelExport" xmi:type="uml:Model">
                    <packagedElement name="ClassA" xmi:id="EAID_1" xmi:type="uml:Class">
                      <ownedAttribute name="attr1" visibility="private" xmi:id="EAID_attr1" xmi:type="uml:Property">
                        <type href="http://schema.omg.org/spec/UML/2.1/uml.xml#String" xmi:type="uml:PrimitiveType"/>
                      </ownedAttribute>
                    </packagedElement>
                  </uml:Model>
                </xmi:XMI>
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(xmi.getBytes()) {
            @Override
            public String getFilename() {
                return "model.xmi";
            }
        });
        body.add("commandId", UUID.randomUUID().toString());
        body.add("participantId", "user1");
        body.add("expectedVersion", initialVersion);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<com.umlcase.infrastructure.web.dto.ImportModelResponse> response = restTemplate.postForEntity(
                "/api/projects/{projectId}/import/xmi",
                requestEntity,
                com.umlcase.infrastructure.web.dto.ImportModelResponse.class,
                projectId
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().modelVersion()).isEqualTo(initialVersion + 1);
        assertThat(response.getBody().classCount()).isEqualTo(1);
        assertThat(response.getBody().relationshipCount()).isEqualTo(0);

        UmlModel finalModel = repository.findByProjectId(projectId).orElseThrow();
        assertThat(finalModel.getVersion()).isEqualTo(initialVersion + 1);
        assertThat(finalModel.getClasses()).hasSize(1);
        assertThat(finalModel.getClasses().get(0).getName()).isEqualTo("ClassA");
        assertThat(finalModel.getClasses().get(0).getAttributes()).hasSize(1);
    }

    @Test
    @DisplayName("Rollback: XML inválido retorna 400 y no muta el modelo")
    void importInvalidXmlRollbacks() {
        UmlModel initialModel = repository.findByProjectId(projectId).orElseThrow();
        long initialVersion = initialModel.getVersion();

        String xmi = "<xmi:XMI><invalid></xmi:XMI>";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(xmi.getBytes()) {
            @Override
            public String getFilename() {
                return "model.xmi";
            }
        });
        body.add("commandId", UUID.randomUUID().toString());
        body.add("participantId", "user1");
        body.add("expectedVersion", initialVersion);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/projects/{projectId}/import/xmi",
                requestEntity,
                String.class,
                projectId
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();

        UmlModel finalModel = repository.findByProjectId(projectId).orElseThrow();
        assertThat(finalModel.getVersion()).isEqualTo(initialVersion);
    }

    @Test
    @DisplayName("Rollback: Versión stale retorna 409 y no muta el modelo")
    void importStaleVersionRollbacks() {
        UmlModel initialModel = repository.findByProjectId(projectId).orElseThrow();
        long initialVersion = initialModel.getVersion();

        String xmi = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xmi:XMI xmlns:xmi="http://schema.omg.org/spec/XMI/2.1" xmlns:uml="http://schema.omg.org/spec/UML/2.1" xmi:version="2.1">
                  <uml:Model name="UmlModelExport" xmi:id="EAID_UmlModelExport" xmi:type="uml:Model"/>
                </xmi:XMI>
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(xmi.getBytes()) {
            @Override
            public String getFilename() {
                return "model.xmi";
            }
        });
        body.add("commandId", UUID.randomUUID().toString());
        body.add("participantId", "user1");
        body.add("expectedVersion", initialVersion - 1); // STALE VERSION

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/projects/{projectId}/import/xmi",
                requestEntity,
                String.class,
                projectId
        );

        assertThat(response.getStatusCode().value()).isEqualTo(409);

        UmlModel finalModel = repository.findByProjectId(projectId).orElseThrow();
        assertThat(finalModel.getVersion()).isEqualTo(initialVersion);
    }
}
