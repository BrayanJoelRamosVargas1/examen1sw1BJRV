package com.umlcase.infrastructure.event;

import com.umlcase.application.event.NodeMovedEvent;
import com.umlcase.application.port.out.diagram.DiagramEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDiagramEventPublisher implements DiagramEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDiagramEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(NodeMovedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
