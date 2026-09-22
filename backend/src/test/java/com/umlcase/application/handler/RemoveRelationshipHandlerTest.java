package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipRemovedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveRelationshipHandlerTest {

    @Mock
    private UmlModelRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RemoveRelationshipHandler handler;

    private UmlModel model;
    private UmlClass sourceClass;
    private UmlClass targetClass;
    private UmlRelationship relationship;

    private UUID projectId;
    private UUID commandId;
    private UUID sourceClassId;
    private UUID targetClassId;
    private UUID relationshipId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        commandId = UUID.randomUUID();
        sourceClassId = UUID.randomUUID();
        targetClassId = UUID.randomUUID();
        relationshipId = UUID.randomUUID();

        model = new UmlModel(UUID.randomUUID(), projectId, 10L);

        sourceClass = new UmlClass(sourceClassId, "Source");
        targetClass = new UmlClass(targetClassId, "Target");

        model.addClass(sourceClass);
        model.addClass(targetClass);

        relationship = new UmlRelationship(
                relationshipId,
                RelationshipType.ASSOCIATION,
                sourceClassId,
                targetClassId,
                "1",
                "*"
        );

        model.addRelationship(relationship);
    }

    @Test
    void shouldRemoveRelationshipAndPublishEvent() {
        // Given
        var command = new UmlCommand.RemoveRelationship(
                commandId,
                projectId,
                "user-1",
                10L,
                relationshipId
        );

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));
        when(repository.save(any(UmlModel.class))).thenAnswer(inv -> {
            UmlModel m = inv.getArgument(0);
            m.setVersion(m.getVersion() + 1); // Simulate Hibernate Optimistic Locking increment
            return m;
        });

        // When
        RelationshipRemovedEvent event = handler.handle(command);

        // Then
        assertThat(model.getVersion()).isEqualTo(11L);
        assertThat(model.getRelationships()).isEmpty();
        assertThat(model.getClasses()).hasSize(2); // classes remain intact

        assertThat(event.relationshipId()).isEqualTo(relationshipId);
        assertThat(event.modelVersion()).isEqualTo(11L);

        verify(repository).save(model);
        verify(eventPublisher).publishEvent(event);
    }

    @Test
    void shouldThrowExceptionWhenRelationshipDoesNotExist() {
        // Given
        var command = new UmlCommand.RemoveRelationship(
                commandId,
                projectId,
                "user-1",
                10L,
                UUID.randomUUID()
        );

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));

        // When/Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró la relación con id");

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenModelVersionConflicts() {
        // Given
        var command = new UmlCommand.RemoveRelationship(
                commandId,
                projectId,
                "user-1",
                9L, // stale
                relationshipId
        );

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));

        // When/Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ModelVersionConflictException.class);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
