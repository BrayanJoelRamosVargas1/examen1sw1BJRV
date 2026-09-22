package com.umlcase.application.port.in;

import com.umlcase.application.command.UpdateOperationCommand;
import com.umlcase.application.event.OperationUpdatedEvent;

public interface UpdateOperationUseCase {
    OperationUpdatedEvent handle(UpdateOperationCommand command);
}
