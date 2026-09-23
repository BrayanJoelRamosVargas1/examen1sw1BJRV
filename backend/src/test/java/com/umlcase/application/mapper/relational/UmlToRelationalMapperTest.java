package com.umlcase.application.mapper.relational;

import com.umlcase.domain.model.*;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.domain.relational.RelationalTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UmlToRelationalMapperTest {

    private UmlToRelationalMapper mapper;

    @BeforeEach
    void setUp() {
        SqlNamingStrategy naming = new SqlNamingStrategy();
        RelationalTypeMapper typeMapper = new RelationalTypeMapper();
        RelationshipRelationalMapper relMapper = new RelationshipRelationalMapper(naming);
        mapper = new UmlToRelationalMapper(naming, typeMapper, relMapper);
    }

    @Test
    void mapEmptyModel_returnsEmptySchema() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        RelationalSchema schema = mapper.mapToRelational(model);
        assertThat(schema.getTables()).isEmpty();
    }

    @Test
    void mapClass_generatesTableWithIdAndColumns() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass cls = new UmlClass(UUID.randomUUID(), "ClienteVIP");
        cls.addAttribute(new UmlAttribute(UUID.randomUUID(), "nombreCompleto", "String", Visibility.PUBLIC, 0));
        cls.addAttribute(new UmlAttribute(UUID.randomUUID(), "activo", "Boolean", Visibility.PRIVATE, 1));
        model.addClass(cls);

        RelationalSchema schema = mapper.mapToRelational(model);
        assertThat(schema.getTables()).hasSize(1);
        RelationalTable table = schema.getTables().get(0);

        assertThat(table.getName()).isEqualTo("cliente_vip");
        assertThat(table.getPrimaryKey().columns()).containsExactly("id");
        
        assertThat(table.getColumns()).hasSize(3);
        assertThat(table.getColumns().get(0).name()).isEqualTo("id");
        assertThat(table.getColumns().get(0).type()).isEqualTo("UUID");
        
        assertThat(table.getColumns().get(1).name()).isEqualTo("nombre_completo");
        assertThat(table.getColumns().get(1).type()).isEqualTo("VARCHAR(255)");
        
        assertThat(table.getColumns().get(2).name()).isEqualTo("activo");
        assertThat(table.getColumns().get(2).type()).isEqualTo("BOOLEAN");
    }

    @Test
    void mapClass_rejectsDuplicateColumnNames() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass cls = new UmlClass(UUID.randomUUID(), "Cliente");
        cls.addAttribute(new UmlAttribute(UUID.randomUUID(), "nombreCompleto", "String", Visibility.PUBLIC, 0));
        cls.addAttribute(new UmlAttribute(UUID.randomUUID(), "nombre_completo", "String", Visibility.PUBLIC, 1));
        model.addClass(cls);

        assertThatThrownBy(() -> mapper.mapToRelational(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Colisión de nombres de columna detectada: nombre_completo");
    }

    @Test
    void mapClass_rejectsDuplicateTableNames() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        model.addClass(new UmlClass(UUID.randomUUID(), "ClienteVIP"));
        model.addClass(new UmlClass(UUID.randomUUID(), "Cliente_VIP"));

        assertThatThrownBy(() -> mapper.mapToRelational(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Colisión de nombres de tabla detectada: cliente_vip");
    }

    @Test
    void mapClass_rejectsVoidAttribute() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass cls = new UmlClass(UUID.randomUUID(), "Class1");
        cls.addAttribute(new UmlAttribute(UUID.randomUUID(), "attr", "void", Visibility.PUBLIC, 0));
        model.addClass(cls);

        assertThatThrownBy(() -> mapper.mapToRelational(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNSUPPORTED_ATTRIBUTE_TYPE: void is not valid for attributes");
    }
}
