package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.AttributeRemovedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RemoveAttributeHandlerTest {
    private UmlModelRepository repository;
    private UmlEventPublisher publisher;
    private RemoveAttributeHandler handler;
    private UUID projectId;
    private UmlModel model;
    private UmlClass umlClass;
    private UUID attributeId;

    @BeforeEach
    void setUp() {
        repository = mock(UmlModelRepository.class);
        publisher = mock(UmlEventPublisher.class);
        handler = new RemoveAttributeHandler(repository, publisher);
        projectId = UUID.randomUUID();
        model = UmlModel.create(projectId);
        umlClass = UmlClass.create("Cliente");
        model.addClass(umlClass);
        attributeId = model.addAttribute(umlClass.getId(), "nombre", "String", Visibility.PRIVATE).getId();
    }

    private UmlCommand.RemoveAttribute command(long version, UUID classId, UUID attrId) {
        return new UmlCommand.RemoveAttribute(UUID.randomUUID(), projectId, "p1", version, classId, attrId);
    }

    @Test
    void removesExactlyOneAttributeAndPublishesSavedVersion() {
        var other = model.addAttribute(umlClass.getId(), "edad", "Integer", Visibility.PUBLIC);
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        when(repository.save(model)).thenAnswer(invocation -> {
            model.setVersion(1);
            return model;
        });
        var command = command(0, umlClass.getId(), attributeId);

        var response = handler.handle(command);

        assertThat(response.commandId()).isEqualTo(command.commandId());
        assertThat(response.classId()).isEqualTo(umlClass.getId());
        assertThat(response.attributeId()).isEqualTo(attributeId);
        assertThat(response.modelVersion()).isEqualTo(1);
        assertThat(umlClass.getAttributes()).extracting(a -> a.getId()).containsExactly(other.getId());
        assertThat(other.getOrderIndex()).isEqualTo(1);
        verify(repository).save(model);
        ArgumentCaptor<AttributeRemovedEvent> event = ArgumentCaptor.forClass(AttributeRemovedEvent.class);
        verify(publisher, times(1)).publish(event.capture());
        assertThat(event.getValue().attributeId()).isEqualTo(attributeId);
        assertThat(event.getValue().modelVersion()).isEqualTo(1);
    }

    @Test
    void rejectsUnknownProjectWithoutEvent() {
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(command(0, umlClass.getId(), attributeId)))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsUnknownClassWithoutSaveOrEvent() {
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        assertThatThrownBy(() -> handler.handle(command(0, UUID.randomUUID(), attributeId)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsUnknownAttributeWithoutSaveOrEvent() {
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        assertThatThrownBy(() -> handler.handle(command(0, umlClass.getId(), UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsStaleVersionWithoutSaveOrEvent() {
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        assertThatThrownBy(() -> handler.handle(command(7, umlClass.getId(), attributeId)))
                .isInstanceOf(ModelVersionConflictException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }
}
