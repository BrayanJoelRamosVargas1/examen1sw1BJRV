package com.umlcase.domain;

import com.umlcase.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests del dominio UML puro.
 *
 * [ADR-001] Estos tests NO necesitan Spring, JPA ni base de datos.
 * El dominio es testeable de forma completamente aislada.
 *
 * Pregunta oral esperada:
 *  - ¿Por qué los tests del dominio no tienen @SpringBootTest?
 *    → Porque el dominio no depende de Spring. Tests rápidos y aislados.
 *  - ¿Qué es AssertJ? → Librería de aserciones fluidas, incluida en Spring Boot Test.
 */
@DisplayName("Dominio UML — Reglas de negocio")
class UmlDomainTest {

    // ─── UmlClass ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Crear una clase UML con nombre válido")
    void createClassWithValidName() {
        UmlClass cliente = UmlClass.create("Cliente");

        assertThat(cliente.getId()).isNotNull();
        assertThat(cliente.getName()).isEqualTo("Cliente");
        assertThat(cliente.getAttributes()).isEmpty();
        assertThat(cliente.getOperations()).isEmpty();
    }

    @Test
    @DisplayName("No se puede crear una clase con nombre vacío")
    void createClassWithEmptyNameThrows() {
        assertThatThrownBy(() -> UmlClass.create(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser vacío");
    }

    @Test
    @DisplayName("No se puede crear una clase con nombre null")
    void createClassWithNullNameThrows() {
        assertThatThrownBy(() -> UmlClass.create(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Agregar un atributo a una clase")
    void addAttributeToClass() {
        UmlClass clase = UmlClass.create("Producto");
        UmlAttribute precio = UmlAttribute.create("precio", "Double", Visibility.PRIVATE);

        clase.addAttribute(precio);

        assertThat(clase.getAttributes()).hasSize(1);
        assertThat(clase.getAttributes().get(0).getName()).isEqualTo("precio");
    }

    @Test
    @DisplayName("No se permiten atributos con nombre duplicado")
    void duplicateAttributeNameThrows() {
        UmlClass clase = UmlClass.create("Producto");
        clase.addAttribute(UmlAttribute.create("precio", "Double"));

        assertThatThrownBy(() -> clase.addAttribute(UmlAttribute.create("precio", "Integer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precio");
    }

    @Test
    @DisplayName("Agregar una operación a una clase")
    void addOperationToClass() {
        UmlClass clase = UmlClass.create("Pedido");
        UmlOperation calcular = UmlOperation.create("calcularTotal", "Double", Visibility.PUBLIC);

        clase.addOperation(calcular);

        assertThat(clase.getOperations()).hasSize(1);
        assertThat(clase.getOperations().get(0).getName()).isEqualTo("calcularTotal");
    }

    @Test
    @DisplayName("Representación UML de un atributo privado")
    void attributeUmlStringRepresentation() {
        UmlAttribute attr = UmlAttribute.create("nombre", "String", Visibility.PRIVATE);
        assertThat(attr.toUmlString()).isEqualTo("- nombre : String");
    }

    @Test
    @DisplayName("Representación UML de una operación pública")
    void operationUmlStringRepresentation() {
        UmlOperation op = UmlOperation.create("getNombre", "String", Visibility.PUBLIC);
        assertThat(op.toUmlString()).isEqualTo("+ getNombre() : String");
    }

    // ─── UmlModel ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Crear un UmlModel vacío")
    void createEmptyModel() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);

        assertThat(model.getId()).isNotNull();
        assertThat(model.getProjectId()).isEqualTo(projectId);
        assertThat(model.getClasses()).isEmpty();
        assertThat(model.getRelationships()).isEmpty();
    }

    @Test
    @DisplayName("Agregar una clase al modelo")
    void addClassToModel() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass cliente = UmlClass.create("Cliente");

        model.addClass(cliente);

        assertThat(model.getClasses()).hasSize(1);
        assertThat(model.findClassByName("Cliente")).isPresent();
    }

    @Test
    @DisplayName("No se permiten clases con nombre duplicado en el modelo")
    void duplicateClassNameInModelThrows() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        model.addClass(UmlClass.create("Pedido"));

        assertThatThrownBy(() -> model.addClass(UmlClass.create("Pedido")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pedido");
    }

    @Test
    @DisplayName("Eliminar una clase elimina también sus relaciones")
    void removeClassRemovesRelationships() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass cliente = UmlClass.create("Cliente");
        UmlClass pedido = UmlClass.create("Pedido");
        model.addClass(cliente);
        model.addClass(pedido);

        UmlRelationship rel = UmlRelationship.create(
                RelationshipType.ASSOCIATION,
                cliente.getId(),
                pedido.getId(),
                "1", "*");
        model.addRelationship(rel);

        assertThat(model.getRelationships()).hasSize(1);

        model.removeClass(cliente.getId());

        assertThat(model.getClasses()).hasSize(1);
        // La relación que involucraba a cliente debe haberse eliminado
        assertThat(model.getRelationships()).isEmpty();
    }

    @Test
    @DisplayName("No se puede crear relación si las clases no existen en el modelo")
    void addRelationshipWithNonExistentClassThrows() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        model.addClass(UmlClass.create("Cliente"));

        UmlRelationship rel = UmlRelationship.create(
                RelationshipType.ASSOCIATION,
                UUID.randomUUID(), // clase que NO existe
                UUID.randomUUID()  // clase que NO existe
        );

        assertThatThrownBy(() -> model.addRelationship(rel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deben existir");
    }

    // ─── UmlNodeView (separación semántica/visual) ────────────────────────────

    @Test
    @DisplayName("[ADR-002] Mover un nodo solo cambia UmlNodeView, no UmlClass")
    void moveNodeDoesNotAffectUmlClass() {
        UmlClass clase = UmlClass.create("Factura");
        UUID elementId = clase.getId();

        UmlNodeView view = UmlNodeView.create(elementId, 0, 0);
        view.moveTo(100, 200);

        // El nombre y atributos de la clase no cambian
        assertThat(clase.getName()).isEqualTo("Factura");
        // Solo la vista cambia
        assertThat(view.getX()).isEqualTo(100);
        assertThat(view.getY()).isEqualTo(200);
        assertThat(view.getElementId()).isEqualTo(elementId);
    }
}
