package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.ClassCreatedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@DisplayName("CreateClassTransactionalIT — Transacciones y Eventos STOMP")
class CreateClassTransactionalIT {

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
    private CreateClassHandler handler;

    @Autowired
    private UmlModelRepository repository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("commit exitoso → 1 CLASS_CREATED disparado por AFTER_COMMIT")
    void handle_successfulCommit_publishesEvent() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UmlModel initialModel = UmlModel.create(projectId);
        repository.save(initialModel);

        UmlCommand.CreateClass command = new UmlCommand.CreateClass(
                UUID.randomUUID(),
                projectId,
                "user-1",
                initialModel.getVersion(),
                "Cliente"
        );

        // Act
        handler.handle(command);

        // Assert: Esperamos hasta 1000ms a que el evento AFTER_COMMIT sea disparado a STOMP
        verify(messagingTemplate, timeout(1000).times(1)).convertAndSend(
                eq("/topic/projects/" + projectId),
                any(ClassCreatedEvent.class)
        );
    }

    @Test
    @DisplayName("rollback → 0 CLASS_CREATED (por error de concurrencia optimista)")
    void handle_rollback_doesNotPublishEvent() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UmlModel initialModel = UmlModel.create(projectId);
        repository.save(initialModel);

        // Act: Intentamos con expectedVersion obsoleto (0 en lugar de la actual, asumamos que la actual fue modificada pero en este caso fallamos enviando una version invalida si queremos)
        // Wait, para forzar OptimisticLocking, pasamos expectedVersion diferente:
        UmlCommand.CreateClass badCommand = new UmlCommand.CreateClass(
                UUID.randomUUID(),
                projectId,
                "user-2",
                initialModel.getVersion() - 1, // Invalid version
                "Pedido"
        );

        // Assert exception
        assertThatThrownBy(() -> handler.handle(badCommand))
                .isInstanceOf(ModelVersionConflictException.class);

        // STOMP no debe ser notificado porque la transaccion falló
        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/projects/" + projectId),
                any(ClassCreatedEvent.class)
        );
    }
}
