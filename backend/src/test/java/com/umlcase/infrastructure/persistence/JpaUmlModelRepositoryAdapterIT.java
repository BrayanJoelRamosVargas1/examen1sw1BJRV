package com.umlcase.infrastructure.persistence;

import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Test de integración: JPA + Flyway + PostgreSQL real (Testcontainers).
 *
 * [ADR-001] Prueba que el adaptador JPA cumple el contrato del puerto.
 *
 * Verifica:
 *  - Flyway aplica V1 correctamente
 *  - findByProjectId funciona
 *  - save() devuelve la versión real post-flush (no version+1 manual)
 *  - @Version protege frente a escrituras concurrentes (OptimisticLock)
 *
 * Pregunta oral esperada:
 *  - ¿Por qué Testcontainers y no H2?
 *    → H2 no emula completamente PostgreSQL. @Version puede comportarse diferente.
 *      Los tests de producción deben correr contra la BD real.
 */
@SpringBootTest
@Testcontainers
@DisplayName("JpaUmlModelRepositoryAdapter — Integración con PostgreSQL real")
class JpaUmlModelRepositoryAdapterIT {

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
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Flyway habilitado con ubicación de migración V1
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private UmlModelRepository repository;

    // ─── Flyway + persistencia básica ─────────────────────────────────────────

    @Test
    @DisplayName("Flyway aplica V1: save y load round-trip correcto")
    void saveAndLoad_roundTrip() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);

        UmlModel saved = repository.save(model);

        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getClasses()).isEmpty();
        // La versión JPA arranca en 0 (primer save)
        assertThat(saved.getVersion()).isGreaterThanOrEqualTo(0L);

        Optional<UmlModel> loaded = repository.findByProjectId(projectId);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getProjectId()).isEqualTo(projectId);
    }

    @Test
    @DisplayName("findByProjectId encuentra el modelo por project_id, no por model.id")
    void findByProjectId_returnsCorrectModel() {
        UUID projectId = UUID.randomUUID();
        UUID modelInternalId = UUID.randomUUID();

        // Creamos el modelo directamente con un id interno diferente al projectId
        UmlModel model = new UmlModel(modelInternalId, projectId, 0L);
        repository.save(model);

        // findByProjectId debe funcionar, findById con projectId debe devolver empty
        assertThat(repository.findByProjectId(projectId)).isPresent();
        assertThat(repository.findById(projectId)).isEmpty(); // projectId != modelInternalId
    }

    @Test
    @DisplayName("save + addClass: versión real incrementada por JPA @Version")
    void save_withClass_versionIncrementedByJpa() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlModel initialSaved = repository.save(model);
        long versionAfterCreate = initialSaved.getVersion();

        // Añadimos clase y volvemos a guardar
        UmlClass cliente = UmlClass.create("Cliente");
        initialSaved.addClass(cliente);
        UmlModel updatedModel = repository.save(initialSaved);

        // La versión debe haber incrementado (JPA @Version lo hace automáticamente)
        assertThat(updatedModel.getVersion()).isGreaterThan(versionAfterCreate);
        // La clase persiste
        assertThat(repository.findByProjectId(projectId).orElseThrow().getClasses())
                .extracting(UmlClass::getName)
                .containsExactly("Cliente");
    }

    @Test
    @DisplayName("La versión devuelta por save() proviene de la BD, no es version+1 manual")
    void save_returnsPersistedVersion_notManualCalculation() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 0L);
        UmlModel saved = repository.save(model);
        long returnedVersion = saved.getVersion();

        // La versión que devuelve save() debe coincidir con la que hay en BD
        UmlModel fromDb = repository.findByProjectId(projectId).orElseThrow();
        assertThat(returnedVersion).isEqualTo(fromDb.getVersion());
    }

    @Test
    @DisplayName("Conflicto de concurrencia: ModelVersionConflictException al escribir versión obsoleta")
    void save_optimisticLock_throwsConflict() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlModel saved = repository.save(model);

        // Simulamos dos lecturas del mismo estado
        UmlModel instance1 = repository.findByProjectId(projectId).orElseThrow();
        UmlModel instance2 = repository.findByProjectId(projectId).orElseThrow();

        // instance1 guarda primero → incrementa versión
        instance1.addClass(UmlClass.create("Cliente"));
        repository.save(instance1);

        // instance2 intenta guardar con la versión obsoleta → conflicto
        instance2.addClass(UmlClass.create("Pedido"));
        assertThatThrownBy(() -> repository.save(instance2))
                .isInstanceOf(ModelVersionConflictException.class);
    }

    @Test
    @Transactional
    @DisplayName("2.1.B: Modificar un atributo de un hijo (renombrar clase) DEBE incrementar la versión usando loadForUpdate")
    void save_renameChildClass_incrementsRootVersion() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass cliente = UmlClass.create("Cliente");
        model.addClass(cliente);
        UmlModel saved = repository.save(model);
        long versionAfterCreate = saved.getVersion();

        // Renombrar la clase usando loadForUpdate (que tiene OPTIMISTIC_FORCE_INCREMENT)
        UmlModel loaded = repository.loadForUpdate(projectId).orElseThrow();
        UmlClass classToRename = loaded.getClasses().stream()
                .filter(c -> c.getName().equals("Cliente"))
                .findFirst().orElseThrow();
        classToRename.rename("ClienteNuevo");

        // Guardar el modelo
        UmlModel updatedModel = repository.save(loaded);

        // Si JPA no detecta el cambio en la fila hija como un cambio en la raíz,
        // el versionado de la raíz no subirá.
        assertThat(updatedModel.getVersion())
                .as("La versión de la raíz debe incrementar tras modificar un hijo")
                .isGreaterThan(versionAfterCreate);
    }
}
