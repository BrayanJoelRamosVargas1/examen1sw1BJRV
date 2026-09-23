package com.umlcase.application.ai;

import java.util.List;

public record UmlImageInterpretation(
    List<InterpretedUmlCommand> commands,
    List<String> warnings
) {
    public UmlImageInterpretation {
        commands = commands == null ? List.of() : List.copyOf(commands);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
