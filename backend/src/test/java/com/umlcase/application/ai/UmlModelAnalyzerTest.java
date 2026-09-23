package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UmlModelAnalyzerTest {
    private final UmlModelAnalyzer analyzer = new UmlModelAnalyzer();

    @Test
    void detectsEmptyModel() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        assertTrue(analyzer.analyze(model).stream().anyMatch(f -> f.code().equals("EMPTY_MODEL")));
    }

    @Test
    void detectsClassWithoutMembersAndIsolatedClass() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        model.addClass(UmlClass.create("Direccion"));
        var codes = analyzer.analyze(model).stream().map(UmlModelFinding::code).toList();
        assertTrue(codes.contains("CLASS_WITHOUT_MEMBERS"));
        assertTrue(codes.contains("ISOLATED_CLASS"));
    }
}
