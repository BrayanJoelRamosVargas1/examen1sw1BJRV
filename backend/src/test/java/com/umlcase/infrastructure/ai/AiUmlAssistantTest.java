package com.umlcase.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.application.ai.UmlModelAnalyzer;
import com.umlcase.application.ai.UmlAssistantResponse;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiUmlAssistantTest {
    @Test
    void disabledProviderReturnsLocalAnalysis() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);
        UmlModel model = UmlModel.create(UUID.randomUUID());
        model.addClass(UmlClass.create("Producto"));

        AiUmlAssistant assistant = new AiUmlAssistant(properties, new ObjectMapper());
        UmlAssistantResponse response = assistant.answer("resume el modelo", model, new UmlModelAnalyzer().analyze(model));

        assertTrue(response.answer().contains("1 clases"));
        assertTrue(response.warnings().get(0).contains("Proveedor IA no configurado"));
    }
}
