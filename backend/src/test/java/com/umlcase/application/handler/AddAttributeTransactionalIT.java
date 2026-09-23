package com.umlcase.application.handler;

import com.umlcase.application.event.AttributeAddedEvent;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.AddAttributeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@DisplayName("AddAttributeTransactionalIT — AFTER_COMMIT events and transactions")
class AddAttributeTransactionalIT {

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
    private AddAttributeHandler handler;

    @Autowired
    private UmlModelRepository repository;

    @MockBean
    private UmlEventPublisher eventPublisher;

    @Test
    @DisplayName("Commit exitoso emite exactamente 1 AttributeAddedEvent con la misma versión persistida")
    void handle_successEmitsEvent() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UmlModel initialModel = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("ClaseTx");
        initialModel.addClass(umlClass);
        initialModel = repository.save(initialModel);
        
        Long initialVersion = initialModel.getVersion();

        com.umlcase.application.command.UmlCommand.AddAttribute command = new com.umlcase.application.command.UmlCommand.AddAttribute(
                UUID.randomUUID(),
                projectId,
                "participant1",
                initialVersion,
                umlClass.getId(),
                "attrTx",
                "int",
                com.umlcase.domain.model.Visibility.PRIVATE
        );

        // Act
        com.umlcase.infrastructure.web.dto.AddAttributeResponse result = handler.handle(command);

        // Assert
        assertThat(result).isNotNull();
        
        UmlModel updatedModel = repository.findByProjectId(projectId).orElseThrow();
        Long newVersion = updatedModel.getVersion();
        assertThat(newVersion).isGreaterThan(initialVersion);

        org.mockito.ArgumentCaptor<AttributeAddedEvent> captor = org.mockito.ArgumentCaptor.forClass(AttributeAddedEvent.class);
        verify(eventPublisher, timeout(1000).times(1)).publish(captor.capture());
        AttributeAddedEvent capturedEvent = captor.getValue();
        assertThat(capturedEvent.commandId()).isEqualTo(command.commandId().toString());
        assertThat(capturedEvent.projectId()).isEqualTo(projectId);
        assertThat(capturedEvent.classId()).isEqualTo(umlClass.getId());
        assertThat(capturedEvent.name()).isEqualTo("attrTx");
        assertThat(capturedEvent.modelVersion()).isEqualTo(newVersion);
    }

    @Test
    @DisplayName("Rollback no emite AttributeAddedEvent")
    void handle_rollbackNoEvent() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UmlModel initialModel = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("ClaseTxFail");
        initialModel.addClass(umlClass);
        repository.save(initialModel);

        com.umlcase.application.command.UmlCommand.AddAttribute command = new com.umlcase.application.command.UmlCommand.AddAttribute(
                UUID.randomUUID(),
                projectId,
                "participant1",
                0L, // stale version to force rollback
                umlClass.getId(),
                "attrTx",
                "int",
                com.umlcase.domain.model.Visibility.PRIVATE
        );

        // Act
        assertThatThrownBy(() -> handler.handle(command));

        // Assert
        verify(eventPublisher, timeout(1000).times(0)).publish(any(AttributeAddedEvent.class));
    }
}
