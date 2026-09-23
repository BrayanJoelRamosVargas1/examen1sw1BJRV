package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.exception.CommandIdReuseException;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.AddAttributeResponse;
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
class AddAttributeIdempotencyIT {

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
    private AddAttributeHandler handler;

    @Autowired
    private UmlModelRepository repository;

    private UUID projectId;
    private UUID classId;
    private Long initialVersion;

    @BeforeEach
    void setUp() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass cls = UmlClass.create("User");
        model.addClass(cls);
        UmlModel saved = repository.save(model);
        projectId = saved.getProjectId();
        classId = cls.getId();
        initialVersion = saved.getVersion();
    }

    @Test
    void shouldReturnSameResultOnIdempotentRetry() {
        UUID commandId = UUID.randomUUID();
        UmlCommand.AddAttribute command = new UmlCommand.AddAttribute(commandId, projectId, "user1", initialVersion, classId, "name", "String", Visibility.PRIVATE);
        
        AddAttributeResponse response1 = handler.handle(command);
        assertThat(response1.attribute().name()).isEqualTo("name");
        assertThat(response1.modelVersion()).isEqualTo(initialVersion + 1L);

        // Retry exactly same
        AddAttributeResponse response2 = handler.handle(command);
        
        // Assertions
        assertThat(response2.attribute().id()).isEqualTo(response1.attribute().id());
        assertThat(response2.modelVersion()).isEqualTo(response1.modelVersion());
        
        // Verify only 1 attribute was created
        UmlModel model = repository.findByProjectId(projectId).orElseThrow();
        UmlClass cls = model.getClasses().stream().filter(c -> c.getId().equals(classId)).findFirst().orElseThrow();
        assertThat(cls.getAttributes()).hasSize(1);
        assertThat(model.getVersion()).isEqualTo(initialVersion + 1L);
    }

    @Test
    void shouldThrowOnCommandIdReuseWithDifferentPayload() {
        UUID commandId = UUID.randomUUID();
        UmlCommand.AddAttribute command1 = new UmlCommand.AddAttribute(commandId, projectId, "user1", initialVersion, classId, "name", "String", Visibility.PRIVATE);
        handler.handle(command1);

        UmlCommand.AddAttribute command2 = new UmlCommand.AddAttribute(commandId, projectId, "user1", initialVersion, classId, "age", "Integer", Visibility.PRIVATE);
        
        assertThatThrownBy(() -> handler.handle(command2))
                .isInstanceOf(CommandIdReuseException.class)
                .hasMessageContaining("Reuso de commandId con diferente payload");
    }
}
