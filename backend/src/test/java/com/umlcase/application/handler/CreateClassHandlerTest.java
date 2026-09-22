package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.ClassCreatedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.CreateClassResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitario del CreateClassHandler.
 *
 * [ADR-001] Sin Spring, sin JPA, sin DB. Completamente aislado.
 *
 * Usa implementaciones fake (doubles) del repositorio y el publisher.
 * Verifica comportamiento observable, NO detalles internos de implementación.
 *
 * Pregunta oral esperada:
 *  - ¿Por qué no usas Mockito? → Porque los fakes son más legibles y suficientes.
 *    También podrían usarse mocks; la diferencia es estilo.
 *  - ¿Qué diferencia hay entre fake y mock? → El fake tiene lógica propia;
 *    el mock simplemente registra y verifica llamadas.
 */
@DisplayName("CreateClassHandler — Comportamiento del caso de uso")
class CreateClassHandlerTest {

    // ── Doubles ──────────────────────────────────────────────────────────────

    private FakeUmlModelRepository repository;
    private FakeUmlEventPublisher publisher;
    private CreateClassHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeUmlModelRepository();
        publisher = new FakeUmlEventPublisher();
        handler = new CreateClassHandler(repository, publisher);
    }

    // ── Escenario: éxito ─────────────────────────────────────────────────────

    @Test
    @DisplayName("handle: modelo guardado contiene la nueva clase, evento usa versión persistida real")
    void handle_success_classAddedAndEventPublished() {
        UUID projectId = UUID.randomUUID();
        // Modelo con versión 3, vacío
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 3L);
        repository.store(model);

        UmlCommand.CreateClass command = new UmlCommand.CreateClass(
                UUID.randomUUID(),
                projectId,
                "participante-A",
                3L,          // expectedVersion correcto
                "Cliente"
        );

        handler.handle(command);

        // El modelo guardado contiene "Cliente"
        UmlModel savedModel = repository.findByProjectId(projectId).orElseThrow();
        assertThat(savedModel.getClasses())
                .extracting(UmlClass::getName)
                .containsExactly("Cliente");

        // El evento fue publicado exactamente una vez
        assertThat(publisher.publishedEvents()).hasSize(1);

        ClassCreatedEvent event = publisher.publishedEvents().get(0);
        assertThat(event.className()).isEqualTo("Cliente");
        assertThat(event.projectId()).isEqualTo(projectId);

        // La versión del evento viene de la persistencia real, no de version+1 manual
        // El fake incrementa la versión en save() simulando el comportamiento de JPA @Version
        assertThat(event.modelVersion()).isGreaterThan(3L);
        assertThat(event.modelVersion()).isEqualTo(savedModel.getVersion());
    }

    // ── Escenario: versión incorrecta ────────────────────────────────────────

    @Test
    @DisplayName("handle: expectedVersion incorrecta → ModelVersionConflictException, no save, no publish")
    void handle_wrongExpectedVersion_throwsConflict() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 5L);
        repository.store(model);

        UmlCommand.CreateClass command = new UmlCommand.CreateClass(
                UUID.randomUUID(),
                projectId,
                "participante-A",
                3L,          // versión incorrecta: la real es 5
                "Cliente"
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ModelVersionConflictException.class);

        // No se guardó nada
        assertThat(repository.saveCount()).isEqualTo(0);
        // No se publicó evento
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    // ── Escenario: proyecto inexistente ──────────────────────────────────────

    @Test
    @DisplayName("handle: proyecto inexistente → ProjectNotFoundException, no save, no publish")
    void handle_projectNotFound_throwsNotFoundException() {
        UUID nonExistentProjectId = UUID.randomUUID();

        UmlCommand.CreateClass command = new UmlCommand.CreateClass(
                UUID.randomUUID(),
                nonExistentProjectId,
                "participante-A",
                0L,
                "Cliente"
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ProjectNotFoundException.class);

        assertThat(repository.saveCount()).isEqualTo(0);
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    /**
     * Repositorio fake en memoria.
     * Simula el comportamiento real de JPA: save() incrementa la versión
     * (como lo hace @Version de Hibernate).
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
            // Simula el incremento de @Version que hace JPA al hacer saveAndFlush
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
     * Publisher fake que registra los eventos publicados.
     */
    static class FakeUmlEventPublisher implements UmlEventPublisher {
        private final List<ClassCreatedEvent> events = new ArrayList<>();

        @Override
        public void publish(ClassCreatedEvent event) {
            events.add(event);
        }

        @Override
        public void publish(com.umlcase.application.event.ClassRenamedEvent event) {
            // Ignorado en estos tests
        }

        @Override
        public void publish(com.umlcase.application.event.AttributeAddedEvent event) {
            // Ignorado en estos tests
        }

        @Override
        public void publish(com.umlcase.application.event.AttributeUpdatedEvent event) {
            // Ignorado en estos tests
        }

        @Override
        public void publish(com.umlcase.application.event.AttributeRemovedEvent event) {
            // Ignorado en estos tests
        }

        List<ClassCreatedEvent> publishedEvents() { return events; }
    }
}
