package com.umlcase.demo;

import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.model.Visibility;

import java.util.UUID;

public final class DemoUmlFixture {
    private DemoUmlFixture() {}

    public static UmlModel create() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass persona = addClass(model, "Persona");
        addAttribute(persona, "nombre", "String");
        UmlClass cliente = addClass(model, "Cliente");
        addAttribute(cliente, "activo", "Boolean");
        UmlClass pedido = addClass(model, "Pedido");
        addAttribute(pedido, "total", "Decimal");
        UmlClass lineaPedido = addClass(model, "LineaPedido");
        addAttribute(lineaPedido, "cantidad", "Integer");
        UmlClass producto = addClass(model, "Producto");
        addAttribute(producto, "nombre", "String");
        addAttribute(producto, "precio", "Decimal");
        UmlClass direccion = addClass(model, "Direccion");
        addAttribute(direccion, "texto", "String");

        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.GENERALIZATION,
            cliente.getId(), persona.getId(), null, null));
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.ASSOCIATION,
            cliente.getId(), pedido.getId(), "1", "0..*"));
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.COMPOSITION,
            pedido.getId(), lineaPedido.getId(), "1", "1..*"));
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.ASSOCIATION,
            lineaPedido.getId(), producto.getId(), "0..*", "1"));
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.AGGREGATION,
            cliente.getId(), direccion.getId(), "1", "0..*"));
        return model;
    }

    private static UmlClass addClass(UmlModel model, String name) {
        UmlClass umlClass = UmlClass.create(name);
        model.addClass(umlClass);
        return umlClass;
    }

    private static void addAttribute(UmlClass umlClass, String name, String type) {
        umlClass.addAttribute(UmlAttribute.create(name, type, Visibility.PUBLIC));
    }
}
