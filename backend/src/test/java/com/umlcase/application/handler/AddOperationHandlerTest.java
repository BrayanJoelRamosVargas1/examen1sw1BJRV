package com.umlcase.application.handler;

import com.umlcase.application.command.AddOperationCommand;
import com.umlcase.application.event.OperationAddedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddOperationHandlerTest {

    private UmlModelRepository repository;
    private UmlEventPublisher eventPublisher;
    private AddOperationHandler handler;

    @BeforeEach
    void setUp() {
        repository = mock(UmlModelRepository.class);
        eventPublisher = mock(UmlEventPublisher.class);
        handler = new AddOperationHandler(repository, eventPublisher);
    }

    @Test
    void handle_success_withParameters() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("User");
        model.addClass(umlClass);

        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        when(repository.save(model)).thenAnswer(inv -> {
            UmlModel m = inv.getArgument(0);
            return new UmlModel(m.getId(), m.getProjectId(), m.getVersion() + 1);
        });

        AddOperationCommand command = AddOperationCommand.builder()
                .commandId(UUID.randomUUID())
                .projectId(projectId)
                .participantId("p1")
                .expectedVersion(0)
                .classId(umlClass.getId())
                .name("calculate")
                .returnType("int")
                .visibility(Visibility.PUBLIC)
                .parameters(List.of(
                        AddOperationCommand.ParameterData.builder().name("a").type("int").build(),
                        AddOperationCommand.ParameterData.builder().name("b").type("int").build()
                ))
                .build();

        var result = handler.handle(command);

        assertEquals(1, result.newModelVersion());
        assertEquals("calculate", result.operation().getName());
        assertEquals(2, result.operation().getParameters().size());
        assertEquals("a", result.operation().getParameters().get(0).getName());
        assertEquals("b", result.operation().getParameters().get(1).getName());

        verify(repository).save(model);
        verify(eventPublisher).publish(any(OperationAddedEvent.class));
    }

    @Test
    void handle_success_noParameters() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("User");
        model.addClass(umlClass);

        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        when(repository.save(model)).thenAnswer(inv -> {
            UmlModel m = inv.getArgument(0);
            return new UmlModel(m.getId(), m.getProjectId(), m.getVersion() + 1);
        });

        AddOperationCommand command = AddOperationCommand.builder()
                .commandId(UUID.randomUUID())
                .projectId(projectId)
                .participantId("p1")
                .expectedVersion(0)
                .classId(umlClass.getId())
                .name("calculate")
                .returnType("int")
                .visibility(Visibility.PUBLIC)
                .build();

        var result = handler.handle(command);

        assertEquals(1, result.newModelVersion());
        assertEquals("calculate", result.operation().getName());
        assertEquals(0, result.operation().getParameters().size());

        verify(repository).save(model);
        verify(eventPublisher).publish(any(OperationAddedEvent.class));
    }

    @Test
    void handle_conflict() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("User");
        model.addClass(umlClass);

        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));

        AddOperationCommand command = AddOperationCommand.builder()
                .projectId(projectId)
                .expectedVersion(999) // Conflict
                .classId(umlClass.getId())
                .name("calculate")
                .returnType("int")
                .visibility(Visibility.PUBLIC)
                .build();

        assertThrows(ModelVersionConflictException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any(OperationAddedEvent.class));
    }
}
