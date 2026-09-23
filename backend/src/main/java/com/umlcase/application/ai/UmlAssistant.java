package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlModel;

public interface UmlAssistant {
    UmlAssistantResponse answer(String message, UmlModel model, java.util.List<UmlModelFinding> findings);
}
