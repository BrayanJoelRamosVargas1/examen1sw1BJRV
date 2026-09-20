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
    public void handleClassCreatedEvent(ClassCreatedEvent event) {
        String topic = "/topic/projects/" + event.projectId();
        messagingTemplate.convertAndSend(topic, event);
    }
}
