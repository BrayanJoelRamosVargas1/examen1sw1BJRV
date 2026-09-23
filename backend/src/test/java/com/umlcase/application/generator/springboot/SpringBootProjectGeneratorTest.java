package com.umlcase.application.generator.springboot;

import com.umlcase.application.mapper.java.JavaNamingStrategy;
import com.umlcase.application.mapper.java.UmlToJavaModelMapper;
import com.umlcase.application.mapper.relational.RelationalMappingContext;
import com.umlcase.application.mapper.relational.SqlNamingStrategy;
import com.umlcase.application.mapper.relational.UmlToRelationalMapper;
import com.umlcase.application.port.in.GeneratedProject;
import com.umlcase.domain.generation.java.GeneratedJavaModel;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.relational.RelationalSchema;
import com.umlcase.infrastructure.interchange.spring.ZipSpringBootProjectExporter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SpringBootProjectGeneratorTest {

    @Autowired
    private ZipSpringBootProjectExporter exporter;

    @Autowired
    private UmlToJavaModelMapper javaMapper;

    @Autowired
    private UmlToRelationalMapper relationalMapper;

    @Autowired
    private com.umlcase.application.port.out.RelationalSchemaExporter sqlExporter;

    @Test
    void generatedProject_canCompileWithJava21() throws Exception {
        // Complex Model Fixture
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 0L);

        var persona = com.umlcase.domain.model.UmlClass.create("Persona");
        model.addClass(persona);
        model.addAttribute(persona.getId(), "nombreCompleto", "String", com.umlcase.domain.model.Visibility.PUBLIC);

        var cliente = com.umlcase.domain.model.UmlClass.create("Cliente");
        model.addClass(cliente);
        model.addAttribute(cliente.getId(), "activo", "Boolean", com.umlcase.domain.model.Visibility.PUBLIC);
        model.addRelationship(new com.umlcase.domain.model.UmlRelationship(
            UUID.randomUUID(), com.umlcase.domain.model.RelationshipType.GENERALIZATION,
            cliente.getId(), persona.getId(), null, null
        ));

        var pedido = com.umlcase.domain.model.UmlClass.create("Pedido");
        model.addClass(pedido);
        model.addAttribute(pedido.getId(), "total", "Decimal", com.umlcase.domain.model.Visibility.PUBLIC);
        
        var linea = com.umlcase.domain.model.UmlClass.create("LineaPedido");
        model.addClass(linea);
        model.addAttribute(linea.getId(), "cantidad", "Integer", com.umlcase.domain.model.Visibility.PUBLIC);

        var producto = com.umlcase.domain.model.UmlClass.create("Producto");
        model.addClass(producto);
        model.addAttribute(producto.getId(), "precio", "Decimal", com.umlcase.domain.model.Visibility.PUBLIC);

        var direccion = com.umlcase.domain.model.UmlClass.create("Direccion");
        model.addClass(direccion);
        model.addAttribute(direccion.getId(), "texto", "String", com.umlcase.domain.model.Visibility.PUBLIC);

        // Relationships
        // 1:N Pedido -> LineaPedido (Composition)
        model.addRelationship(new com.umlcase.domain.model.UmlRelationship(
            UUID.randomUUID(), com.umlcase.domain.model.RelationshipType.COMPOSITION,
            pedido.getId(), linea.getId(), "1", "0..*"
        ));
        
        // 1:N Cliente -> Pedido
        model.addRelationship(new com.umlcase.domain.model.UmlRelationship(
            UUID.randomUUID(), com.umlcase.domain.model.RelationshipType.ASSOCIATION,
            cliente.getId(), pedido.getId(), "1", "0..*"
        ));
        
        // N:1 LineaPedido -> Producto
        model.addRelationship(new com.umlcase.domain.model.UmlRelationship(
            UUID.randomUUID(), com.umlcase.domain.model.RelationshipType.ASSOCIATION,
            linea.getId(), producto.getId(), "0..*", "1"
        ));
        
        // 1:1 Cliente -> Direccion
        model.addRelationship(new com.umlcase.domain.model.UmlRelationship(
            UUID.randomUUID(), com.umlcase.domain.model.RelationshipType.COMPOSITION,
            cliente.getId(), direccion.getId(), "1", "1"
        ));

        // 1. Map to Relational and Java
        RelationalSchema schema = relationalMapper.mapToRelational(model);
        GeneratedJavaModel javaModel = javaMapper.mapToJavaModel(model);
        byte[] sql = sqlExporter.exportToSql(schema);

        // 2. Export ZIP
        GeneratedProject project = exporter.exportToZip(javaModel, sql);

        // 3. Unzip to temporary directory
        Path tempDir = Files.createTempDirectory("generated-backend");
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(project.content()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(tempDir.toFile(), entry.getName());
                if (entry.getName().endsWith("/")) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        zis.transferTo(fos);
                    }
                }
            }
        }

        // 4. Verify compilation!
        File projectDir = new File(tempDir.toFile(), "generated-backend");
        
        // Use Docker to compile the project with Java 21 as requested by the user
        String absolutePath = projectDir.getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "--rm", 
                "-v", absolutePath + ":/usr/src/mymaven", 
                "-w", "/usr/src/mymaven", 
                "maven:3.9-eclipse-temurin-21", 
                "mvn", "clean", "test"
        );
        pb.directory(projectDir);
        File buildLog = new File(projectDir, "build.log");
        pb.redirectOutput(buildLog);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            String output = java.nio.file.Files.readString(buildLog.toPath());
            System.err.println("BUILD FAILED:\n" + output);
        }

        assertThat(exitCode).isEqualTo(0).withFailMessage("El proyecto generado no compila o fallan sus tests");
    }
}
