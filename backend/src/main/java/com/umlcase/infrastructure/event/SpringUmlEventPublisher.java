package com.umlcase.infrastructure.event;

import com.umlcase.application.event.ClassCreatedEvent;
import com.umlcase.application.port.out.UmlEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringUmlEventPublisher implements UmlEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringUmlEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(ClassCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(com.umlcase.application.event.ClassRenamedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
