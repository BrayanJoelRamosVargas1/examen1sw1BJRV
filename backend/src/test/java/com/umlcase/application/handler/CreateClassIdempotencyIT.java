package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.exception.CommandIdReuseException;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.CreateClassResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CreateClassIdempotencyIT {

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
    private CreateClassHandler handler;

    @Autowired
    private UmlModelRepository repository;

    private UUID projectId;
    private Long initialVersion;

    @BeforeEach
    void setUp() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlModel saved = repository.save(model);
        projectId = saved.getProjectId();
        initialVersion = saved.getVersion();
    }

    @Test
    void shouldReturnSameResultOnIdempotentRetry() {
        UUID commandId = UUID.randomUUID();
        UmlCommand.CreateClass command = new UmlCommand.CreateClass(commandId, projectId, "user1", initialVersion, "MyClass");
        
        CreateClassResponse response1 = handler.handle(command);
        assertThat(response1.className()).isEqualTo("MyClass");
        assertThat(response1.modelVersion()).isEqualTo(initialVersion + 1L);

        // Retry exactly same
        CreateClassResponse response2 = handler.handle(command);
        
        // Assertions
        assertThat(response2.classId()).isEqualTo(response1.classId());
        assertThat(response2.modelVersion()).isEqualTo(response1.modelVersion());
        
        // Verify only 1 class was created
        UmlModel model = repository.findByProjectId(projectId).orElseThrow();
        assertThat(model.getClasses()).hasSize(1);
        assertThat(model.getVersion()).isEqualTo(initialVersion + 1L);
    }

    @Test
    void shouldThrowOnCommandIdReuseWithDifferentPayload() {
        UUID commandId = UUID.randomUUID();
        UmlCommand.CreateClass command1 = new UmlCommand.CreateClass(commandId, projectId, "user1", initialVersion, "ClassA");
        handler.handle(command1);

        UmlCommand.CreateClass command2 = new UmlCommand.CreateClass(commandId, projectId, "user1", initialVersion, "ClassB");
        
        assertThatThrownBy(() -> handler.handle(command2))
                .isInstanceOf(CommandIdReuseException.class)
                .hasMessageContaining("Reuso de commandId con diferente payload");
    }
}
