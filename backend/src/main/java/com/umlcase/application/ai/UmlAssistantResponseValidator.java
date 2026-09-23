package com.umlcase.application.ai;

import org.springframework.stereotype.Component;

@Component
public class UmlAssistantResponseValidator {
    public boolean isValid(UmlAssistantResponse response) {
        return response != null
            && response.answer() != null
            && response.answer().length() <= 4000
            && response.suggestedCommands() != null
            && response.suggestedCommands().size() <= 10;
    }
}
