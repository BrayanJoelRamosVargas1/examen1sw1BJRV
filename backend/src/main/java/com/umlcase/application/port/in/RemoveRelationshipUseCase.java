package com.umlcase.application.port.in;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipRemovedEvent;

public interface RemoveRelationshipUseCase {
    RelationshipRemovedEvent handle(UmlCommand.RemoveRelationship command);
}
