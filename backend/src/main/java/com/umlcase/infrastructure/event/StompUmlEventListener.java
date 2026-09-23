package com.umlcase.infrastructure.event;

import com.umlcase.application.event.ClassCreatedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StompUmlEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public StompUmlEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleClassCreatedEvent(com.umlcase.application.event.ClassCreatedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleClassRenamedEvent(com.umlcase.application.event.ClassRenamedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAttributeAddedEvent(com.umlcase.application.event.AttributeAddedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAttributeUpdatedEvent(com.umlcase.application.event.AttributeUpdatedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAttributeRemovedEvent(com.umlcase.application.event.AttributeRemovedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOperationAddedEvent(com.umlcase.application.event.OperationAddedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOperationUpdatedEvent(com.umlcase.application.event.OperationUpdatedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOperationRemovedEvent(com.umlcase.application.event.OperationRemovedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRelationshipAddedEvent(com.umlcase.application.event.RelationshipAddedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRelationshipUpdatedEvent(com.umlcase.application.event.RelationshipUpdatedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRelationshipRemovedEvent(com.umlcase.application.event.RelationshipRemovedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(destination, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleModelImportedEvent(com.umlcase.application.event.ModelImportedEvent event) {
        String destination = "/topic/projects/" + event.projectId();
        // Agregamos un field eventType explícito para el payload STOMP, si el frontend lo requiere para discriminar.
        // Spring convierte los records a JSON directamente.
        messagingTemplate.convertAndSend(destination, new Object() {
            public final String eventType = "MODEL_IMPORTED";
            public final String commandId = event.commandId();
            public final String participantId = event.participantId();
            public final java.util.UUID projectId = event.projectId();
            public final int modelVersion = event.modelVersion();
        });
    }
}
