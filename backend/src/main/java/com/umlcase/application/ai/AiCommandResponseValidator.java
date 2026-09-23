package com.umlcase.application.ai;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.function.Predicate;

@Component
public class AiCommandResponseValidator {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "CREATE_CLASS", "RENAME_CLASS", "ADD_ATTRIBUTE", "ADD_OPERATION", "ADD_RELATIONSHIP"
    );
    private static final Set<String> ALLOWED_DATA_TYPES = Set.of("String", "Integer", "Decimal", "Boolean");
    private static final Set<String> ALLOWED_REL_TYPES = Set.of("ASSOCIATION", "AGGREGATION", "COMPOSITION", "GENERALIZATION");
    private static final Set<String> ALLOWED_MULTIPLICITIES = Set.of("1", "*", "0..*", "1..*", "0..1");

    public boolean isValid(InterpretedUmlCommand cmd) {
        if (cmd == null || cmd.type() == null) return false;
        if (!ALLOWED_TYPES.contains(cmd.type())) return false;

        return switch (cmd.type()) {
            case "CREATE_CLASS" -> present(cmd.className());
            case "RENAME_CLASS" -> present(cmd.className()) && present(cmd.newClassName());
            case "ADD_ATTRIBUTE" -> present(cmd.className()) && present(cmd.attributeName())
                && allowed(cmd.attributeType(), ALLOWED_DATA_TYPES);
            case "ADD_OPERATION" -> present(cmd.className()) && present(cmd.operationName());
            case "ADD_RELATIONSHIP" -> present(cmd.sourceClass()) && present(cmd.targetClass())
                && allowed(cmd.relationshipType(), ALLOWED_REL_TYPES)
                && allowed(cmd.sourceMultiplicity(), ALLOWED_MULTIPLICITIES)
                && allowed(cmd.targetMultiplicity(), ALLOWED_MULTIPLICITIES);
            default -> false;
        };
    }

    private boolean allowed(String value, Set<String> allowedValues) {
        return value != null && allowedValues.contains(value);
    }

    private boolean present(String value) {
        return value != null && !value.isBlank() && value.length() <= 200;
    }
}
