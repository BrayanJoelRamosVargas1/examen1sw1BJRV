package com.umlcase.application.port.in;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipAddedEvent;

public interface AddRelationshipUseCase {
    RelationshipAddedEvent handle(UmlCommand.AddRelationship command);
}
