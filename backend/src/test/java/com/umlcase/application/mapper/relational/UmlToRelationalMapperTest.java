package com.umlcase.application.mapper.relational;

import com.umlcase.domain.model.*;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.domain.relational.RelationalTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    void testConcurrentExecution_noStatePollution() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    UmlModel model = UmlModel.create(UUID.randomUUID());
                    model.addClass(new UmlClass(UUID.randomUUID(), "TestTable" + index));
                    RelationalSchema schema = mapper.mapToRelational(model);
                    assertThat(schema.getTables()).hasSize(1);
                    assertThat(schema.getTables().get(0).getName()).isEqualTo("test_table" + index);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
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

    @Test
    void mapRelationships_1_N_addsFkToTarget() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass c1 = new UmlClass(UUID.randomUUID(), "Cliente");
        UmlClass c2 = new UmlClass(UUID.randomUUID(), "Pedido");
        model.addClass(c1);
        model.addClass(c2);
        
        UmlRelationship r = new UmlRelationship(UUID.randomUUID(), RelationshipType.ASSOCIATION, c1.getId(), c2.getId(), "1", "0..*");
        model.addRelationship(r);
        
        RelationalSchema schema = mapper.mapToRelational(model);
        RelationalTable pedidoTable = schema.getTables().stream().filter(t -> t.getName().equals("pedido")).findFirst().get();
        assertThat(pedidoTable.getColumns().stream().anyMatch(c -> c.name().equals("cliente_id"))).isTrue();
        assertThat(pedidoTable.getForeignKeys()).hasSize(1);
    }
    
    @Test
    void mapRelationships_N_M_createsJoinTable() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass c1 = new UmlClass(UUID.randomUUID(), "Estudiante");
        UmlClass c2 = new UmlClass(UUID.randomUUID(), "Curso");
        model.addClass(c1);
        model.addClass(c2);
        
        UmlRelationship r = new UmlRelationship(UUID.randomUUID(), RelationshipType.ASSOCIATION, c1.getId(), c2.getId(), "0..*", "0..*");
        model.addRelationship(r);
        
        RelationalSchema schema = mapper.mapToRelational(model);
        assertThat(schema.getTables()).hasSize(3);
        RelationalTable joinTable = schema.getTables().stream().filter(t -> t.getName().equals("curso_estudiante") || t.getName().equals("estudiante_curso")).findFirst().get();
        assertThat(joinTable.getColumns()).hasSize(2);
        assertThat(joinTable.getForeignKeys()).hasSize(2);
    }
    
    @Test
    void mapRelationships_1_1_addsFkWithUniqueConstraint() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass c1 = new UmlClass(UUID.randomUUID(), "Cliente");
        UmlClass c2 = new UmlClass(UUID.randomUUID(), "Perfil");
        model.addClass(c1);
        model.addClass(c2);
        
        UmlRelationship r = new UmlRelationship(UUID.randomUUID(), RelationshipType.ASSOCIATION, c1.getId(), c2.getId(), "0..1", "1");
        model.addRelationship(r);
        
        RelationalSchema schema = mapper.mapToRelational(model);
        RelationalTable clienteTable = schema.getTables().stream().filter(t -> t.getName().equals("cliente")).findFirst().get();
        assertThat(clienteTable.getColumns().stream().anyMatch(c -> c.name().equals("perfil_id"))).isTrue();
        assertThat(clienteTable.getUniqueConstraints()).hasSize(1);
    }
    
    @Test
    void mapRelationships_compositionAddsCascade() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass c1 = new UmlClass(UUID.randomUUID(), "Factura");
        UmlClass c2 = new UmlClass(UUID.randomUUID(), "Linea");
        model.addClass(c1);
        model.addClass(c2);
        
        UmlRelationship r = new UmlRelationship(UUID.randomUUID(), RelationshipType.COMPOSITION, c1.getId(), c2.getId(), "1", "0..*");
        model.addRelationship(r);
        
        RelationalSchema schema = mapper.mapToRelational(model);
        RelationalTable lineaTable = schema.getTables().stream().filter(t -> t.getName().equals("linea")).findFirst().get();
        assertThat(lineaTable.getForeignKeys().get(0).onDeleteCascade()).isTrue();
    }
    
    @Test
    void mapRelationships_generalizationCycleRejected() {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass c1 = new UmlClass(UUID.randomUUID(), "A");
        UmlClass c2 = new UmlClass(UUID.randomUUID(), "B");
        model.addClass(c1);
        model.addClass(c2);
        
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.GENERALIZATION, c1.getId(), c2.getId(), null, null));
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.GENERALIZATION, c2.getId(), c1.getId(), null, null));
        
        assertThatThrownBy(() -> mapper.mapToRelational(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ciclo de herencia detectado");
    }
}
