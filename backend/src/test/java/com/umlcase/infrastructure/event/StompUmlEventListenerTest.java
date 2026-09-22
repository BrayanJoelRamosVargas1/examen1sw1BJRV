package com.umlcase.infrastructure.event;

import com.umlcase.application.event.RelationshipAddedEvent;
import com.umlcase.domain.model.RelationshipType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompUmlEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private StompUmlEventListener listener;

    @Test
    void shouldForwardRelationshipAddedEventToStompTopic() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        RelationshipAddedEvent event = new RelationshipAddedEvent(
                UUID.randomUUID(),
                projectId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                RelationshipType.ASSOCIATION,
                "1",
                "*" ,
                2L
        );

        // Act
        listener.handleRelationshipAddedEvent(event);

        // Assert
        verify(messagingTemplate).convertAndSend("/topic/projects/" + projectId, event);
    }

    @Test
    void shouldForwardRelationshipUpdatedEventToStompTopic() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        com.umlcase.application.event.RelationshipUpdatedEvent event = new com.umlcase.application.event.RelationshipUpdatedEvent(
                UUID.randomUUID(),
                projectId,
                UUID.randomUUID(),
                RelationshipType.AGGREGATION,
                "1",
                "*" ,
                3L
        );

        // Act
        listener.handleRelationshipUpdatedEvent(event);

        // Assert
        verify(messagingTemplate).convertAndSend("/topic/projects/" + projectId, event);
    }
}
