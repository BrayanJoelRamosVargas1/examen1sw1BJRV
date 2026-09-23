package com.umlcase.infrastructure.web;

import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class GenerateRelationalSchemaApiIT {

    

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UmlModelRepository repository;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.addClass(new UmlClass(UUID.randomUUID(), "TestClass"));
        repository.save(model);
    }

    @AfterEach
    void tearDown() {
        repository.findByProjectId(projectId).ifPresent(m -> repository.deleteById(m.getId()));
    }

    @Test
    void getRelationalSchema_returnsSchemaDto() {
        long versionBefore = repository.findByProjectId(projectId).orElseThrow().getVersion();
        ResponseEntity<com.umlcase.infrastructure.web.dto.relational.RelationalSchemaResponse> response =
                restTemplate.getForEntity("/api/projects/{projectId}/relational-schema", com.umlcase.infrastructure.web.dto.relational.RelationalSchemaResponse.class, projectId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tables()).hasSize(1);
        assertThat(response.getBody().tables().get(0).name()).isEqualTo("test_class");
        
        long versionAfter = repository.findByProjectId(projectId).orElseThrow().getVersion();
        assertThat(versionAfter).isEqualTo(versionBefore);
    }
}
