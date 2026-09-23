package com.umlcase.infrastructure.interchange.xmi;

import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.model.UmlRelationship;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UmlModelSemanticAssert {

    public static void assertModelsSemanticallyEquivalent(UmlModel original, UmlModel imported) {
        assertThat(imported.getClasses()).hasSameSizeAs(original.getClasses());

        // Sort both by name to compare them easily (assuming names are unique in tests)
        List<UmlClass> origClasses = original.getClasses().stream()
                .sorted(Comparator.comparing(UmlClass::getName))
                .toList();
        List<UmlClass> impClasses = imported.getClasses().stream()
                .sorted(Comparator.comparing(UmlClass::getName))
                .toList();

        for (int i = 0; i < origClasses.size(); i++) {
            assertClassesEquivalent(origClasses.get(i), impClasses.get(i));
        }

        assertThat(imported.getRelationships()).hasSameSizeAs(original.getRelationships());
        
        // Relationships comparison
        // We match them by sourceClass/targetClass type and relationship type
        for (UmlRelationship origRel : original.getRelationships()) {
            boolean found = imported.getRelationships().stream().anyMatch(impRel -> 
                impRel.getType() == origRel.getType() &&
                impRel.getSourceMultiplicity().equals(origRel.getSourceMultiplicity()) &&
                impRel.getTargetMultiplicity().equals(origRel.getTargetMultiplicity()) &&
                getOriginalClassName(original, origRel.getSourceClassId()).equals(getOriginalClassName(imported, impRel.getSourceClassId())) &&
                getOriginalClassName(original, origRel.getTargetClassId()).equals(getOriginalClassName(imported, impRel.getTargetClassId()))
            );
            if (!found) {
                System.out.println("Looking for: " + origRel.getType() + " srcMult=" + origRel.getSourceMultiplicity() + " tgtMult=" + origRel.getTargetMultiplicity() + " srcClass=" + getOriginalClassName(original, origRel.getSourceClassId()) + " tgtClass=" + getOriginalClassName(original, origRel.getTargetClassId()));
                for (UmlRelationship impRel : imported.getRelationships()) {
                    System.out.println("Found: " + impRel.getType() + " srcMult=" + impRel.getSourceMultiplicity() + " tgtMult=" + impRel.getTargetMultiplicity() + " srcClass=" + getOriginalClassName(imported, impRel.getSourceClassId()) + " tgtClass=" + getOriginalClassName(imported, impRel.getTargetClassId()));
                }
            }
            assertThat(found)
                .as("Relationship " + origRel.getType() + " missing in imported model")
                .isTrue();
        }
    }

    private static String getOriginalClassName(UmlModel model, java.util.UUID classId) {
        return model.getClasses().stream()
                .filter(c -> c.getId().equals(classId))
                .findFirst()
                .map(UmlClass::getName)
                .orElse("UNKNOWN");
    }

    private static void assertClassesEquivalent(UmlClass orig, UmlClass imp) {
        assertThat(imp.getName()).isEqualTo(orig.getName());
        assertThat(imp.getAttributes()).hasSameSizeAs(orig.getAttributes());

        for (int i = 0; i < orig.getAttributes().size(); i++) {
            var oAttr = orig.getAttributes().get(i);
            var iAttr = imp.getAttributes().get(i);
            assertThat(iAttr.getName()).isEqualTo(oAttr.getName());
            assertThat(iAttr.getType()).isEqualTo(oAttr.getType());
            assertThat(iAttr.getVisibility()).isEqualTo(oAttr.getVisibility());
        }

        assertThat(imp.getOperations()).hasSameSizeAs(orig.getOperations());

        for (int i = 0; i < orig.getOperations().size(); i++) {
            UmlOperation oOp = orig.getOperations().get(i);
            UmlOperation iOp = imp.getOperations().get(i);
            
            assertThat(iOp.getName()).isEqualTo(oOp.getName());
            assertThat(iOp.getReturnType()).isEqualTo(oOp.getReturnType());
            assertThat(iOp.getVisibility()).isEqualTo(oOp.getVisibility());
            assertThat(iOp.getParameters()).hasSameSizeAs(oOp.getParameters());

            for (int j = 0; j < oOp.getParameters().size(); j++) {
                var oParam = oOp.getParameters().get(j);
                var iParam = iOp.getParameters().get(j);
                
                assertThat(iParam.getName()).isEqualTo(oParam.getName());
                assertThat(iParam.getType()).isEqualTo(oParam.getType());
            }
        }
    }
}
