package com.umlcase.application.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UmlAssistantResponseValidatorTest {
    private final UmlAssistantResponseValidator validator = new UmlAssistantResponseValidator();

    @Test
    void acceptsStructuredResponseWithinLimits() {
        assertTrue(validator.isValid(new UmlAssistantResponse("Resumen", List.of(), List.of(), List.of())));
    }

    @Test
    void rejectsOversizedAnswerAndTooManyCommands() {
        assertFalse(validator.isValid(new UmlAssistantResponse("x".repeat(4001), List.of(), List.of(), List.of())));
        var commands = java.util.stream.IntStream.range(0, 11)
            .mapToObj(i -> new InterpretedUmlCommand("CREATE_CLASS", "C" + i, null, null, null, null, null, null, null, null, null))
            .toList();
        assertFalse(validator.isValid(new UmlAssistantResponse("Resumen", List.of(), commands, List.of())));
    }
}
