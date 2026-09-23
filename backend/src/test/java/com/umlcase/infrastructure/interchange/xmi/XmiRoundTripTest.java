package com.umlcase.infrastructure.interchange.xmi;

import com.umlcase.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@DisplayName("XmiRoundTripTest — Validaciones Semánticas XMI")
class XmiRoundTripTest {

    private final XmiExportAdapter exporter = new XmiExportAdapter();
    private final XmiImportAdapter importer = new XmiImportAdapter();

    @Test
    @DisplayName("Round-trip de modelo vacío")
    void roundTripEmpty() {
        UmlModel original = UmlModel.create(UUID.randomUUID());
        
        byte[] xmiA = exporter.export(original);
        UmlModel modelB = importer.importModel(xmiA);
        
        UmlModelSemanticAssert.assertModelsSemanticallyEquivalent(original, modelB);
        
        byte[] xmiB = exporter.export(modelB);
        UmlModel modelC = importer.importModel(xmiB);
        
        UmlModelSemanticAssert.assertModelsSemanticallyEquivalent(modelB, modelC);
    }

    @Test
    @DisplayName("Round-trip modelo completo mixto")
    void roundTripFullMixed() {
        UmlModel original = UmlModel.create(UUID.randomUUID());

        // Clase A
        UmlClass classA = new UmlClass(UUID.randomUUID(), "ClassA");
        classA.addAttribute(new UmlAttribute(UUID.randomUUID(), "attr1", "String", Visibility.PRIVATE, 0));
        
        UmlOperation opA = new UmlOperation(UUID.randomUUID(), "op1", "Integer", Visibility.PUBLIC, 0);
        opA.addParameter(new UmlParameter(UUID.randomUUID(), "p1", "String", 0));
        classA.addOperation(opA);

        // Clase B
        UmlClass classB = new UmlClass(UUID.randomUUID(), "ClassB");
        classB.addAttribute(new UmlAttribute(UUID.randomUUID(), "attr2", "Boolean", Visibility.PROTECTED, 0));

        // Clase C
        UmlClass classC = new UmlClass(UUID.randomUUID(), "ClassC");

        original.addClass(classA);
        original.addClass(classB);
        original.addClass(classC);

        // Relaciones
        UmlRelationship rel1 = new UmlRelationship(UUID.randomUUID(), RelationshipType.AGGREGATION, classA.getId(), classB.getId(), "1", "*");
        UmlRelationship rel2 = new UmlRelationship(UUID.randomUUID(), RelationshipType.GENERALIZATION, classB.getId(), classC.getId(), "1", "1");

        original.addRelationship(rel1);
        original.addRelationship(rel2);

        // 1. Export -> Import
        byte[] xmiA = exporter.export(original);
        UmlModel modelB = importer.importModel(xmiA);
        
        UmlModelSemanticAssert.assertModelsSemanticallyEquivalent(original, modelB);
        
        // 2. Export -> Import
        byte[] xmiB = exporter.export(modelB);
        UmlModel modelC = importer.importModel(xmiB);
        
        UmlModelSemanticAssert.assertModelsSemanticallyEquivalent(modelB, modelC);
    }
}
