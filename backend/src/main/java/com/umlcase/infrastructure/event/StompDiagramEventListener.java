package com.umlcase.infrastructure.event;

import com.umlcase.application.event.NodeMovedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StompDiagramEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public StompDiagramEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNodeMovedEvent(NodeMovedEvent event) {
        String topic = "/topic/projects/" + event.getProjectId() + "/diagram";
        messagingTemplate.convertAndSend(topic, event);
    }
}
