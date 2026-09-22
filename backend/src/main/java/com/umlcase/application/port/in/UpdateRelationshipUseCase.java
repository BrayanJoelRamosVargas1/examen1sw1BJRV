package com.umlcase.application.port.in;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipUpdatedEvent;

public interface UpdateRelationshipUseCase {
    RelationshipUpdatedEvent handle(UmlCommand.UpdateRelationship command);
}
