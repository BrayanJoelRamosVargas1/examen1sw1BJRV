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
}
