package com.umlcase.application.port.out.diagram;

import com.umlcase.application.event.NodeMovedEvent;

public interface DiagramEventPublisher {
    void publish(NodeMovedEvent event);
}
