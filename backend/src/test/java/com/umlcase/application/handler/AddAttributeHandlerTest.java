package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.event.AttributeAddedEvent;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.AddAttributeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AddAttributeHandler — Pruebas de aplicación y reglas de negocio")
class AddAttributeHandlerTest {

    private UmlModelRepository repository;
    private UmlEventPublisher eventPublisher;
    private AddAttributeHandler handler;

    @BeforeEach
    void setUp() {
        repository = mock(UmlModelRepository.class);
        eventPublisher = mock(UmlEventPublisher.class);
        handler = new AddAttributeHandler(repository, eventPublisher);
    }

    @Test
    @DisplayName("Agrega el atributo correctamente y emite el evento, asignando max(orderIndex)+1")
    void handle_success() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.setVersion(5L);
        UmlClass umlClass = UmlClass.create("ClaseTest");
        model.addClass(umlClass);
        model.addAttribute(umlClass.getId(), "attr1", "String", Visibility.PRIVATE);

        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        when(repository.save(any(UmlModel.class))).thenAnswer(i -> {
            UmlModel m = i.getArgument(0);
            m.setVersion(m.getVersion() + 1);
            return m;
        });

        UmlCommand.AddAttribute command = new UmlCommand.AddAttribute(
                UUID.randomUUID(),
                projectId,
                "participant1",
                5L,
                umlClass.getId(),
                "attr2",
                "int",
                Visibility.PUBLIC
        );

        // Act
        AddAttributeResponse result = handler.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.attribute().name()).isEqualTo("attr2");
        assertThat(result.attribute().type()).isEqualTo("int");
        assertThat(result.attribute().visibility()).isEqualTo("PUBLIC");
        assertThat(result.attribute().orderIndex()).isEqualTo(1); // 0 + 1 = 1

        verify(repository).save(model);
        verify(eventPublisher).publish(any(AttributeAddedEvent.class));
        assertThat(model.getVersion()).isEqualTo(6L);
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si hay duplicado case-insensitive")
    void handle_duplicateName() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("ClaseTest");
        model.addClass(umlClass);
        model.addAttribute(umlClass.getId(), "attr1", "String", Visibility.PRIVATE);

        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));

        UmlCommand.AddAttribute command = new UmlCommand.AddAttribute(
                UUID.randomUUID(),
                projectId,
                "participant1",
                0L,
                umlClass.getId(),
                "AtTr1",
                "int",
                Visibility.PUBLIC
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un atributo con nombre");

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any(AttributeAddedEvent.class));
    }

    @Test
    @DisplayName("Lanza ProjectNotFoundException si el proyecto no existe")
    void handle_projectNotFound() {
        UUID projectId = UUID.randomUUID();
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.empty());

        UmlCommand.AddAttribute command = new UmlCommand.AddAttribute(
                UUID.randomUUID(),
                projectId,
                "participant1",
                0L,
                UUID.randomUUID(),
                "attr",
                "int",
                Visibility.PUBLIC
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Lanza ModelVersionConflictException si expectedVersion es stale")
    void handle_staleVersion() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.setVersion(5L);
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));

        UmlCommand.AddAttribute command = new UmlCommand.AddAttribute(
                UUID.randomUUID(),
                projectId,
                "participant1",
                4L, // stale
                UUID.randomUUID(),
                "attr",
                "int",
                Visibility.PUBLIC
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ModelVersionConflictException.class);
    }
}
