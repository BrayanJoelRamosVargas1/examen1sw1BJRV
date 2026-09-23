package com.umlcase.infrastructure.interchange.sql;

import com.umlcase.domain.model.*;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.application.mapper.relational.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlDdlExporterTest {

    private PostgreSqlDdlExporter exporter;
    private UmlToRelationalMapper mapper;

    @BeforeEach
    void setUp() {
        exporter = new PostgreSqlDdlExporter();
        SqlNamingStrategy naming = new SqlNamingStrategy();
        mapper = new UmlToRelationalMapper(naming, new RelationalTypeMapper(), new RelationshipRelationalMapper(naming));
    }

    @Test
    void generateDdl_isDeterministic() throws NoSuchAlgorithmException {
        UmlModel model = UmlModel.create(UUID.randomUUID());
        UmlClass c1 = new UmlClass(UUID.randomUUID(), "Factura");
        c1.addAttribute(new UmlAttribute(UUID.randomUUID(), "total", "Decimal", Visibility.PUBLIC, 0));
        UmlClass c2 = new UmlClass(UUID.randomUUID(), "Linea");
        c2.addAttribute(new UmlAttribute(UUID.randomUUID(), "cantidad", "Integer", Visibility.PUBLIC, 0));
        model.addClass(c1);
        model.addClass(c2);
        
        model.addRelationship(new UmlRelationship(UUID.randomUUID(), RelationshipType.COMPOSITION, c1.getId(), c2.getId(), "1", "0..*"));

        RelationalSchema schema1 = mapper.mapToRelational(model);
        String sql1 = new String(exporter.exportToSql(schema1));

        RelationalSchema schema2 = mapper.mapToRelational(model);
        String sql2 = new String(exporter.exportToSql(schema2));
        
        assertThat(sql1).isEqualTo(sql2);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String hash1 = Base64.getEncoder().encodeToString(md.digest(sql1.getBytes()));
        String hash2 = Base64.getEncoder().encodeToString(md.digest(sql2.getBytes()));
        assertThat(hash1).isEqualTo(hash2);
    }
}
