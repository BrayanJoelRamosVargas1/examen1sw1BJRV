package com.umlcase.application.port.in.diagram;

import com.umlcase.application.command.diagram.SaveNodeViewCommand;

public interface SaveNodeViewUseCase {
    long execute(SaveNodeViewCommand command);
}
