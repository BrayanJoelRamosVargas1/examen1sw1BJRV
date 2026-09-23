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
class ExportSqlApiIT {

    

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UmlModelRepository repository;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.addClass(new UmlClass(UUID.randomUUID(), "TestClassForSql"));
        repository.save(model);
    }

    @AfterEach
    void tearDown() {
        repository.findByProjectId(projectId).ifPresent(m -> repository.deleteById(m.getId()));
    }

    @Test
    void getExportSql_returnsSqlDdl() {
        long versionBefore = repository.findByProjectId(projectId).orElseThrow().getVersion();
        ResponseEntity<String> response = restTemplate.getForEntity("/api/projects/{projectId}/export/sql", String.class, projectId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("CREATE TABLE test_class_for_sql");
        assertThat(response.getBody()).contains("id UUID NOT NULL");
        assertThat(response.getBody()).contains("PRIMARY KEY (id)");
        
        long versionAfter = repository.findByProjectId(projectId).orElseThrow().getVersion();
        assertThat(versionAfter).isEqualTo(versionBefore);
    }
}
