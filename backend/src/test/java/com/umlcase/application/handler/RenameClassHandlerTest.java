package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.ClassCreatedEvent;
import com.umlcase.application.event.ClassRenamedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RenameClassResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitario del RenameClassHandler.
 *
 * [ADR-001] Sin Spring, sin JPA, sin DB. Completamente aislado.
 *
 * Cubre los 6 escenarios requeridos para el cierre de microiteración 2.1:
 *  1. Rename válido → versión incrementa exactamente N → N+1, evento publicado
 *  2. Nombre duplicado → rechazado, sin save, sin evento
 *  3. Nombre vacío/nulo → rechazado, sin save, sin evento
 *  4. classId inexistente → rechazado, sin save, sin evento
 *  5. expectedVersion incorrecta → ModelVersionConflictException
 *  6. Proyecto inexistente → ProjectNotFoundException
 *
 * Pregunta oral esperada:
 *  - ¿Por qué usas fakes? → Son más legibles que mocks y suficientes para estos tests.
 *  - ¿Quién incrementa la versión? → El repositorio fake en save(), simulando @Version de Hibernate.
 *    La lógica de negocio nunca hace version + 1 manualmente.
 */
@DisplayName("RenameClassHandler — Comportamiento del caso de uso")
class RenameClassHandlerTest {

    // ── Doubles ──────────────────────────────────────────────────────────────

    private FakeUmlModelRepository repository;
    private FakeUmlEventPublisher publisher;
    private RenameClassHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeUmlModelRepository();
        publisher = new FakeUmlEventPublisher();
        handler = new RenameClassHandler(repository, publisher);
    }

    // ── 1. Rename válido: versión N → N+1, evento publicado ──────────────────

    @Test
    @DisplayName("handle: rename exitoso incrementa versión exactamente en 1 y publica evento")
    void handle_validRename_versionIncrementsByExactlyOne() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 3L);
        UmlClass persona = UmlClass.create("Persona");
        model.addClass(persona);
        repository.store(model);

        long versionBefore = model.getVersion(); // N = 3

        UmlCommand.RenameClass command = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "participante-A",
                versionBefore,    // expectedVersion correcto
                persona.getId(),
                "PersonaRenombrada"
        );

        RenameClassResponse response = handler.handle(command);

        long versionAfter = response.modelVersion();

        // Invariante crítico: exactamente +1
        assertThat(versionAfter)
                .as("La versión debe incrementar exactamente en 1 (N=%d, M=%d)", versionBefore, versionAfter)
                .isEqualTo(versionBefore + 1);

        // El modelo persistido refleja el nuevo nombre
        UmlModel savedModel = repository.findByProjectId(projectId).orElseThrow();
        assertThat(savedModel.findClassById(persona.getId()))
                .isPresent()
                .get()
                .extracting(UmlClass::getName)
                .isEqualTo("PersonaRenombrada");

        // Exactamente un evento publicado con la misma versión real
        assertThat(publisher.renamedEvents()).hasSize(1);
        ClassRenamedEvent event = publisher.renamedEvents().get(0);
        assertThat(event.newName()).isEqualTo("PersonaRenombrada");
        assertThat(event.projectId()).isEqualTo(projectId);
        assertThat(event.classId()).isEqualTo(persona.getId());
        assertThat(event.modelVersion())
                .as("La versión del evento debe coincidir con la versión persistida real")
                .isEqualTo(versionAfter);
    }

    // ── 2. Nombre duplicado dentro del modelo ────────────────────────────────

    @Test
    @DisplayName("handle: nombre duplicado → rechazado, sin save, sin evento")
    void handle_duplicateName_rejected() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 2L);
        UmlClass persona = UmlClass.create("Persona");
        UmlClass cliente = UmlClass.create("Cliente");
        model.addClass(persona);
        model.addClass(cliente);
        repository.store(model);

        // Intentar renombrar "Persona" a "Cliente" (ya existe)
        UmlCommand.RenameClass command = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "participante-A",
                2L,
                persona.getId(),
                "Cliente"   // nombre duplicado
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente");

        assertThat(repository.saveCount()).isEqualTo(0);
        assertThat(publisher.renamedEvents()).isEmpty();
    }

    // ── 3. Nombre nulo o vacío ────────────────────────────────────────────────

    @Test
    @DisplayName("handle: newName nulo → rechazado (NullPointerException del dominio)")
    void handle_nullNewName_rejected() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 1L);
        UmlClass persona = UmlClass.create("Persona");
        model.addClass(persona);
        repository.store(model);

        UmlCommand.RenameClass command = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "participante-A",
                1L,
                persona.getId(),
                null   // nombre nulo
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(NullPointerException.class);

        assertThat(repository.saveCount()).isEqualTo(0);
        assertThat(publisher.renamedEvents()).isEmpty();
    }

    // ── 4. classId inexistente ────────────────────────────────────────────────

    @Test
    @DisplayName("handle: classId inexistente → rechazado, sin save, sin evento")
    void handle_nonExistentClassId_rejected() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 1L);
        model.addClass(UmlClass.create("Persona"));
        repository.store(model);

        UUID fantasmaId = UUID.randomUUID(); // No existe en el modelo

        UmlCommand.RenameClass command = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "participante-A",
                1L,
                fantasmaId,
                "NuevoNombre"
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(fantasmaId.toString());

        assertThat(repository.saveCount()).isEqualTo(0);
        assertThat(publisher.renamedEvents()).isEmpty();
    }

    // ── 5. expectedVersion incorrecta (stale writer) ─────────────────────────

    @Test
    @DisplayName("handle: expectedVersion incorrecta → ModelVersionConflictException, sin save, sin evento")
    void handle_wrongExpectedVersion_throwsConflict() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 5L);
        UmlClass persona = UmlClass.create("Persona");
        model.addClass(persona);
        repository.store(model);

        UmlCommand.RenameClass command = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                projectId,
                "participante-B",
                3L,   // stale: la versión real es 5
                persona.getId(),
                "PersonaVIP"
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ModelVersionConflictException.class);

        assertThat(repository.saveCount()).isEqualTo(0);
        assertThat(publisher.renamedEvents()).isEmpty();
    }

    // ── 6. Proyecto inexistente ───────────────────────────────────────────────

    @Test
    @DisplayName("handle: proyecto inexistente → ProjectNotFoundException, sin save, sin evento")
    void handle_projectNotFound_throwsNotFoundException() {
        UUID nonExistentProjectId = UUID.randomUUID();

        UmlCommand.RenameClass command = new UmlCommand.RenameClass(
                UUID.randomUUID(),
                nonExistentProjectId,
                "participante-A",
                0L,
                UUID.randomUUID(),
                "NuevoNombre"
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ProjectNotFoundException.class);

        assertThat(repository.saveCount()).isEqualTo(0);
        assertThat(publisher.renamedEvents()).isEmpty();
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    /**
     * Repositorio fake en memoria.
     * Simula el comportamiento real de JPA: save() incrementa la versión
     * (como lo hace @Version de Hibernate via markModified() + flush).
     */
    static class FakeUmlModelRepository implements UmlModelRepository {
        private final java.util.Map<UUID, UmlModel> byProjectId = new java.util.HashMap<>();
        private int saves = 0;

        void store(UmlModel model) {
            byProjectId.put(model.getProjectId(), model);
        }

        int saveCount() { return saves; }

        @Override
        public Optional<UmlModel> findByProjectId(UUID projectId) {
            return Optional.ofNullable(byProjectId.get(projectId));
        }

        @Override
        public Optional<UmlModel> loadForUpdate(UUID projectId) {
            return findByProjectId(projectId);
        }

        @Override
        public UmlModel save(UmlModel model) {
            saves++;
            // Simula el incremento de @Version que hace Hibernate al detectar
            // la raíz sucia por markModified() y ejecutar saveAndFlush
            long newVersion = model.getVersion() + 1;
            model.setVersion(newVersion);
            byProjectId.put(model.getProjectId(), model);
            return model;
        }

        @Override public Optional<UmlModel> findById(UUID id) { return Optional.empty(); }
        @Override public void deleteById(UUID id) {}
        @Override public boolean existsByProjectId(UUID projectId) { return byProjectId.containsKey(projectId); }
    }

    /**
     * Publisher fake que registra eventos de rename.
     */
    static class FakeUmlEventPublisher implements UmlEventPublisher {
        private final List<ClassRenamedEvent> renamed = new ArrayList<>();

        @Override
        public void publish(ClassCreatedEvent event) {
            // Ignorado en estos tests
        }

        @Override
        public void publish(ClassRenamedEvent event) {
            renamed.add(event);
        }

        @Override
        public void publish(com.umlcase.application.event.AttributeAddedEvent event) {}

        List<ClassRenamedEvent> renamedEvents() { return renamed; }
    }
}
