package com.umlcase.application.handler;

import com.umlcase.application.command.UpdateAttributeCommand;
import com.umlcase.application.event.AttributeUpdatedEvent;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UpdateAttributeHandlerTest {

    private UmlModelRepository repository;
    private UmlEventPublisher publisher;
    private UpdateAttributeHandler handler;

    @BeforeEach
    void setUp() {
        repository = mock(UmlModelRepository.class);
        publisher = mock(UmlEventPublisher.class);
        handler = new UpdateAttributeHandler(repository, publisher);
    }

    @Test
    void shouldUpdateAttributeSuccessfully() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazz = UmlClass.create("TestClass");
        model.addClass(clazz);
        UmlAttribute attr = model.addAttribute(clazz.getId(), "oldName", "String", Visibility.PRIVATE);
        
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        when(repository.save(any(UmlModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAttributeCommand command = UpdateAttributeCommand.builder()
                .commandId(UUID.randomUUID())
                .projectId(projectId)
                .participantId("p1")
                .expectedVersion(0)
                .classId(clazz.getId())
                .attributeId(attr.getId())
                .name("newName")
                .type("Integer")
                .visibility(Visibility.PUBLIC)
                .build();

        handler.handle(command);

        verify(repository).save(model);
        
        assertThat(attr.getName()).isEqualTo("newName");
        assertThat(attr.getType()).isEqualTo("Integer");
        assertThat(attr.getVisibility()).isEqualTo(Visibility.PUBLIC);

        ArgumentCaptor<AttributeUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(AttributeUpdatedEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        
        AttributeUpdatedEvent event = eventCaptor.getValue();
        assertThat(event.name()).isEqualTo("newName");
        assertThat(event.type()).isEqualTo("Integer");
        assertThat(event.visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(event.attributeId()).isEqualTo(attr.getId());
    }

    @Test
    void shouldThrowConflictWhenVersionsMismatch() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));

        UpdateAttributeCommand command = UpdateAttributeCommand.builder()
                .commandId(UUID.randomUUID())
                .projectId(projectId)
                .participantId("p1")
                .expectedVersion(999)
                .classId(UUID.randomUUID())
                .attributeId(UUID.randomUUID())
                .name("name")
                .type("String")
                .visibility(Visibility.PRIVATE)
                .build();

        assertThrows(ModelVersionConflictException.class, () -> handler.handle(command));
    }
}
