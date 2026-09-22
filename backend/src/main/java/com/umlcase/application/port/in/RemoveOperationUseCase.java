package com.umlcase.application.port.in;

import com.umlcase.application.command.RemoveOperationCommand;
import com.umlcase.application.event.OperationRemovedEvent;

public interface RemoveOperationUseCase {
    OperationRemovedEvent handle(RemoveOperationCommand command);
}
