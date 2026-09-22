package com.umlcase.application.port.out;

import com.umlcase.application.event.ClassCreatedEvent;

public interface UmlEventPublisher {
    void publish(com.umlcase.application.event.ClassCreatedEvent event);
    void publish(com.umlcase.application.event.ClassRenamedEvent event);
    void publish(com.umlcase.application.event.AttributeAddedEvent event);
    void publish(com.umlcase.application.event.AttributeUpdatedEvent event);
    void publish(com.umlcase.application.event.AttributeRemovedEvent event);
    void publish(com.umlcase.application.event.OperationAddedEvent event);
    void publish(com.umlcase.application.event.OperationUpdatedEvent event);
    void publish(com.umlcase.application.event.OperationRemovedEvent event);
}
