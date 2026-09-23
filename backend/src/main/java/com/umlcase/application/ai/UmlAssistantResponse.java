package com.umlcase.application.ai;

import java.util.List;

public record UmlAssistantResponse(
    String answer,
    List<UmlModelFinding> findings,
    List<InterpretedUmlCommand> suggestedCommands,
    List<String> warnings
) {
    public UmlAssistantResponse {
        answer = answer == null ? "" : answer;
        findings = findings == null ? List.of() : List.copyOf(findings);
        suggestedCommands = suggestedCommands == null ? List.of() : List.copyOf(suggestedCommands);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
