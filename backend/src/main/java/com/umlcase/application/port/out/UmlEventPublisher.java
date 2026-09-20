package com.umlcase.application.port.out;

import com.umlcase.application.event.ClassCreatedEvent;

public interface UmlEventPublisher {
    void publish(ClassCreatedEvent event);
}
