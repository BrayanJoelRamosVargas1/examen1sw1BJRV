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
}
