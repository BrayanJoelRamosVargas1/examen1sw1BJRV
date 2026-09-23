package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlModel;
import java.util.List;

public interface AiCommandInterpreter {
    List<InterpretedUmlCommand> interpret(String text, UmlModel contextModel);
}
