package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipAddedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddRelationshipHandlerTest {

    private UmlModelRepository repository;
    private ApplicationEventPublisher eventPublisher;
    private AddRelationshipHandler handler;

    @BeforeEach
    void setUp() {
        repository = mock(UmlModelRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        handler = new AddRelationshipHandler(repository, eventPublisher);
    }

    @Test
    void handle_success() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass classA = UmlClass.create("ClassA");
        UmlClass classB = UmlClass.create("ClassB");
        model.addClass(classA);
        model.addClass(classB);

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));
        when(repository.save(model)).thenAnswer(inv -> {
            UmlModel m = inv.getArgument(0);
            return new UmlModel(m.getId(), m.getProjectId(), m.getVersion() + 1);
        });

        UmlCommand.AddRelationship command = new UmlCommand.AddRelationship(
                UUID.randomUUID(), projectId, "p1", 0, RelationshipType.ASSOCIATION,
                classA.getId(), classB.getId(), "1", "*"
        );

        var result = handler.handle(command);

        assertEquals(1, result.modelVersion());
        assertEquals(RelationshipType.ASSOCIATION, result.relationshipType());
        assertEquals(classA.getId(), result.sourceClassId());
        assertEquals(classB.getId(), result.targetClassId());
        assertEquals("1", result.sourceMultiplicity());
        assertEquals("*", result.targetMultiplicity());

        verify(repository).save(model);
        verify(eventPublisher).publishEvent(any(RelationshipAddedEvent.class));
    }

    @Test
    void handle_conflict() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass classA = UmlClass.create("ClassA");
        UmlClass classB = UmlClass.create("ClassB");
        model.addClass(classA);
        model.addClass(classB);

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));

        UmlCommand.AddRelationship command = new UmlCommand.AddRelationship(
                UUID.randomUUID(), projectId, "p1", 999, RelationshipType.ASSOCIATION,
                classA.getId(), classB.getId(), "1", "*"
        );

        assertThrows(ModelVersionConflictException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(RelationshipAddedEvent.class));
    }
}
