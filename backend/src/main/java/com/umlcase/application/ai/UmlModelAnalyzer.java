package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class UmlModelAnalyzer {
    private static final Set<String> SUPPORTED_ATTRIBUTE_TYPES = Set.of("String", "Integer", "Decimal", "Boolean");

    public List<UmlModelFinding> analyze(UmlModel model) {
        List<UmlModelFinding> findings = new ArrayList<>();
        if (model.getClasses().isEmpty()) {
            findings.add(new UmlModelFinding("EMPTY_MODEL", UmlModelFinding.Severity.WARNING,
                "El modelo no contiene clases.", null));
            return findings;
        }

        Set<UUID> relatedClasses = new HashSet<>();
        for (UmlRelationship relationship : model.getRelationships()) {
            relatedClasses.add(relationship.getSourceClassId());
            relatedClasses.add(relationship.getTargetClassId());
            if (isBlank(relationship.getSourceMultiplicity()) || isBlank(relationship.getTargetMultiplicity())) {
                findings.add(new UmlModelFinding("RELATIONSHIP_WITHOUT_MULTIPLICITY", UmlModelFinding.Severity.WARNING,
                    "La relación no tiene multiplicidades completas.", relationship.getId().toString()));
            }
        }

        for (UmlClass umlClass : model.getClasses()) {
            if (umlClass.getAttributes().isEmpty() && umlClass.getOperations().isEmpty()) {
                findings.add(new UmlModelFinding("CLASS_WITHOUT_MEMBERS", UmlModelFinding.Severity.INFO,
                    "La clase no tiene atributos ni operaciones.", umlClass.getName()));
            }
            if (!relatedClasses.contains(umlClass.getId())) {
                findings.add(new UmlModelFinding("ISOLATED_CLASS", UmlModelFinding.Severity.INFO,
                    "La clase no participa en relaciones.", umlClass.getName()));
            }
            umlClass.getAttributes().stream()
                .filter(attribute -> !SUPPORTED_ATTRIBUTE_TYPES.contains(attribute.getType()))
                .forEach(attribute -> findings.add(new UmlModelFinding(
                    "ATTRIBUTE_WITH_UNSUPPORTED_GENERATION_TYPE", UmlModelFinding.Severity.WARNING,
                    "El tipo de atributo no está en los tipos soportados para generación.",
                    umlClass.getName() + "." + attribute.getName())));
        }
        return findings;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
