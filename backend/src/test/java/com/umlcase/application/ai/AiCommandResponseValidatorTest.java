package com.umlcase.application.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiCommandResponseValidatorTest {

    private final AiCommandResponseValidator validator = new AiCommandResponseValidator();

    @Test
    void testValidCreateClass() {
        InterpretedUmlCommand cmd = new InterpretedUmlCommand("CREATE_CLASS", "Cliente", null, null, null, null, null, null, null, null, null);
        assertTrue(validator.isValid(cmd));
    }

    @Test
    void testInvalidType() {
        InterpretedUmlCommand cmd = new InterpretedUmlCommand("DELETE_ALL", "Cliente", null, null, null, null, null, null, null, null, null);
        assertFalse(validator.isValid(cmd));
    }

    @Test
    void testValidAddAttribute() {
        InterpretedUmlCommand cmd = new InterpretedUmlCommand("ADD_ATTRIBUTE", "Cliente", null, "edad", "Integer", null, null, null, null, null, null);
        assertTrue(validator.isValid(cmd));
    }

    @Test
    void testInvalidAttributeType() {
        InterpretedUmlCommand cmd = new InterpretedUmlCommand("ADD_ATTRIBUTE", "Cliente", null, "edad", "UnknownType", null, null, null, null, null, null);
        assertFalse(validator.isValid(cmd));
    }

    @Test
    void testValidRelationship() {
        InterpretedUmlCommand cmd = new InterpretedUmlCommand("ADD_RELATIONSHIP", null, null, null, null, null, "ASSOCIATION", "Cliente", "Pedido", "1", "0..*");
        assertTrue(validator.isValid(cmd));
    }

    @Test
    void testInvalidRelationshipMultiplicity() {
        InterpretedUmlCommand cmd = new InterpretedUmlCommand("ADD_RELATIONSHIP", null, null, null, null, null, "ASSOCIATION", "Cliente", "Pedido", "invalid", "0..*");
        assertFalse(validator.isValid(cmd));
    }

    @Test
    void testRejectsIncompleteCommand() {
        InterpretedUmlCommand cmd = new InterpretedUmlCommand("CREATE_CLASS", null, null, null, null, null, null, null, null, null, null);
        assertFalse(validator.isValid(cmd));
    }
}
