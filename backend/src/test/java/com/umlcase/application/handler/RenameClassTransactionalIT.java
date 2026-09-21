package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.ClassRenamedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RenameClassResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Test de integración transaccional para RenameClass.
 *
 * Demuestra los comportamientos críticos de microiteración 2.1 contra PostgreSQL real:
 *
 *  A. GET (findByProjectId) NO incrementa la versión del modelo
 *  B. Rename incrementa exactamente N → N+1 via @Version de Hibernate
 *  C. Rename con commit exitoso publica exactamente 1 ClassRenamedEvent (AFTER_COMMIT)
 *  D. Stale writer (expectedVersion obsoleta) produce ModelVersionConflictException / HTTP 409
 *  E. Conflicto/rollback publica 0 ClassRenamedEvent
 *
 * [DECISIÓN DE DISEÑO] V2__add_last_modified.sql:
 *   markModified() provoca que la raíz uml_models quede sucia para JPA.
 *   Hibernate ejecuta UPDATE uml_models SET version = version+1, last_modified = ?
 *   La lógica de negocio nunca calcula version+1 manualmente.
 */
@SpringBootTest
@Testcontainers
@DisplayName("RenameClassTransactionalIT — Versionado y Eventos STOMP con PostgreSQL")
class RenameClassTransactionalIT {

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
    private RenameClassHandler handler;

    @Autowired
    private UmlModelRepository repository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    // ── A: GET no incrementa versión ─────────────────────────────────────────

    @Test
    @DisplayName("A: GET repetido no incrementa la versión del modelo")
    void getRepeated_doesNotIncrementVersion() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        repository.save(model);

        // Simula el @Version inicial después del primer save (debería ser 1)
        UmlModel loaded1 = repository.findByProjectId(projectId).orElseThrow();
        long versionAfterFirstSave = loaded1.getVersion();

        // GETs repetidos
        UmlModel loaded2 = repository.findByProjectId(projectId).orElseThrow();
        UmlModel loaded3 = repository.findByProjectId(projectId).orElseThrow();

        assertThat(loaded2.getVersion())
                .as("GET no debe cambiar la versión")
                .isEqualTo(versionAfterFirstSave);
        assertThat(loaded3.getVersion())
                .as("GET repetido no debe cambiar la versión")
                .isEqualTo(versionAfterFirstSave);
    }

    // ── B: Rename incrementa exactamente N → N+1 ────────────────────────────

    @Test
    @DisplayName("B: rename exitoso produce exactamente N → N+1 (versionBefore + 1 == versionAfter)")
    void handle_rename_versionIncrementsExactlyByOne() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.addClass(UmlClass.create("ClienteFase1"));
        repository.save(model);

        UmlModel loaded = repository.findByProjectId(projectId).orElseThrow();
        long versionBefore = loaded.getVersion(); // N
        UUID classId = loaded.getClasses().get(0).getId();

        UmlCommand.RenameClass command = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "user-A",
                versionBefore,
                classId,
                "ClienteVIP"
        );

        RenameClassResponse response = handler.handle(command);
        long versionAfter = response.modelVersion(); // M

        assertThat(versionAfter)
                .as("versionBefore=%d, versionAfter=%d: debe ser N+1", versionBefore, versionAfter)
                .isEqualTo(versionBefore + 1);

        // Verificar persistencia real en PostgreSQL
        UmlModel persisted = repository.findByProjectId(projectId).orElseThrow();
        assertThat(persisted.getVersion()).isEqualTo(versionAfter);
        assertThat(persisted.findClassById(classId))
                .isPresent()
                .get()
                .extracting(UmlClass::getName)
                .isEqualTo("ClienteVIP");
    }

    // ── C: AFTER_COMMIT: commit exitoso → exactamente 1 ClassRenamedEvent ───

    @Test
    @DisplayName("C: commit exitoso → exactamente 1 ClassRenamedEvent vía AFTER_COMMIT")
    void handle_successfulCommit_publishesExactlyOneRenameEvent() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.addClass(UmlClass.create("Pedido"));
        repository.save(model);

        UmlModel loaded = repository.findByProjectId(projectId).orElseThrow();
        UUID classId = loaded.getClasses().get(0).getId();

        UmlCommand.RenameClass command = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "user-1",
                loaded.getVersion(),
                classId,
                "PedidoVIP"
        );

        handler.handle(command);

        // Exactamente 1 evento ClassRenamedEvent enviado por STOMP tras el commit
        verify(messagingTemplate, timeout(1000).times(1)).convertAndSend(
                eq("/topic/projects/" + projectId),
                any(ClassRenamedEvent.class)
        );
    }

    // ── D: Stale writer → 409 MODEL_VERSION_CONFLICT ─────────────────────────

    @Test
    @DisplayName("D: stale writer (expectedVersion obsoleta) → ModelVersionConflictException")
    void handle_staleWriter_throwsConflict() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.addClass(UmlClass.create("Inventario"));
        repository.save(model);

        UmlModel loaded = repository.findByProjectId(projectId).orElseThrow();
        UUID classId = loaded.getClasses().get(0).getId();
        long currentVersion = loaded.getVersion();

        // Usuario B intenta renombrar con versión obsoleta
        UmlCommand.RenameClass staleCommand = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "user-B",
                currentVersion - 1, // versión obsoleta — stale writer
                classId,
                "InventarioRenombrado"
        );

        assertThatThrownBy(() -> handler.handle(staleCommand))
                .isInstanceOf(ModelVersionConflictException.class);
    }

    // ── E: Rollback por conflicto → 0 ClassRenamedEvent ─────────────────────

    @Test
    @DisplayName("E: conflicto/rollback → 0 ClassRenamedEvent publicados")
    void handle_conflictRollback_doesNotPublishRenameEvent() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.addClass(UmlClass.create("Producto"));
        repository.save(model);

        UmlModel loaded = repository.findByProjectId(projectId).orElseThrow();
        UUID classId = loaded.getClasses().get(0).getId();

        // Comando con versión incorrecta (rollback por conflicto)
        UmlCommand.RenameClass badCommand = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "user-X",
                loaded.getVersion() - 1,
                classId,
                "ProductoX"
        );

        assertThatThrownBy(() -> handler.handle(badCommand))
                .isInstanceOf(ModelVersionConflictException.class);

        // STOMP no debe recibir nada porque la transacción no confirmó
        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/projects/" + projectId),
                any(ClassRenamedEvent.class)
        );
    }
}
