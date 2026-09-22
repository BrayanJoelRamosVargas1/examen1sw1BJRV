package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipUpdatedEvent;
import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class UpdateRelationshipHandlerTest {

    @Mock
    private UmlModelRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UpdateRelationshipHandler handler;

    private UUID projectId;
    private UmlModel model;
    private UUID class1;
    private UUID class2;
    private UmlRelationship relationship;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        model = UmlModel.create(projectId);
        model.setVersion(5L);
        class1 = UUID.randomUUID();
        class2 = UUID.randomUUID();
        
        // Mock the findClassById conceptually by adding classes?
        // Wait, addRelationship in UmlModel validates that classes exist.
        model.addClass(com.umlcase.domain.model.UmlClass.create("C1"));
        model.addClass(com.umlcase.domain.model.UmlClass.create("C2"));
        // Hack the ids for the test
        UUID id1 = model.getClasses().get(0).getId();
        UUID id2 = model.getClasses().get(1).getId();
        
        relationship = UmlRelationship.create(RelationshipType.ASSOCIATION, id1, id2);
        model.addRelationship(relationship);
    }

    @Test
    void shouldUpdateRelationshipAndPublishEvent() {
        // Arrange
        UUID cmdId = UUID.randomUUID();
        var command = new UmlCommand.UpdateRelationship(
                cmdId, projectId, "user", 5L, relationship.getId(),
                RelationshipType.AGGREGATION, "1", "*"
        );

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));
        when(repository.save(model)).thenAnswer(inv -> {
            UmlModel m = inv.getArgument(0);
            m.setVersion(m.getVersion() + 1); // Simulate Hibernate Optimistic Locking increment
            return m;
        });

        // Act
        RelationshipUpdatedEvent event = handler.handle(command);

        // Assert
        assertThat(relationship.getType()).isEqualTo(RelationshipType.AGGREGATION);
        assertThat(relationship.getSourceMultiplicity()).isEqualTo("1");
        assertThat(relationship.getTargetMultiplicity()).isEqualTo("*");

        verify(repository).save(model);

        ArgumentCaptor<RelationshipUpdatedEvent> captor = ArgumentCaptor.forClass(RelationshipUpdatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        RelationshipUpdatedEvent published = captor.getValue();
        assertThat(published.commandId()).isEqualTo(cmdId);
        assertThat(published.projectId()).isEqualTo(projectId);
        assertThat(published.relationshipId()).isEqualTo(relationship.getId());
        assertThat(published.type()).isEqualTo(RelationshipType.AGGREGATION);
        assertThat(published.modelVersion()).isEqualTo(6L);
    }

    @Test
    void shouldThrowWhenExpectedVersionDoesNotMatch() {
        // Arrange
        var command = new UmlCommand.UpdateRelationship(
                UUID.randomUUID(), projectId, "user", 4L, relationship.getId(),
                RelationshipType.AGGREGATION, "1", "*"
        );

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(ModelVersionConflictException.class);
                
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
