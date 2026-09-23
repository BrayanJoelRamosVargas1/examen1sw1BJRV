package com.umlcase.infrastructure.interchange.spring;

import com.umlcase.application.generator.springboot.*;
import com.umlcase.application.port.in.GeneratedProject;
import com.umlcase.application.port.out.SpringBootProjectExporter;
import com.umlcase.domain.generation.java.GeneratedJavaEntity;
import com.umlcase.domain.generation.java.GeneratedJavaModel;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ZipSpringBootProjectExporter implements SpringBootProjectExporter {

    private final PomXmlGenerator pomXmlGenerator;
    private final EntityGenerator entityGenerator;
    private final RepositoryGenerator repositoryGenerator;
    private final DtoGenerator dtoGenerator;
    private final ServiceGenerator serviceGenerator;
    private final ControllerGenerator controllerGenerator;
    private final ApplicationClassGenerator appGenerator;
    private final ExceptionHandlerGenerator exceptionGenerator;
    private final ResourceLoader resourceLoader;

    public ZipSpringBootProjectExporter(PomXmlGenerator pomXmlGenerator,
                                        EntityGenerator entityGenerator,
                                        RepositoryGenerator repositoryGenerator,
                                        DtoGenerator dtoGenerator,
                                        ServiceGenerator serviceGenerator,
                                        ControllerGenerator controllerGenerator,
                                        ApplicationClassGenerator appGenerator,
                                        ExceptionHandlerGenerator exceptionGenerator,
                                        ResourceLoader resourceLoader) {
        this.pomXmlGenerator = pomXmlGenerator;
        this.entityGenerator = entityGenerator;
        this.repositoryGenerator = repositoryGenerator;
        this.dtoGenerator = dtoGenerator;
        this.serviceGenerator = serviceGenerator;
        this.controllerGenerator = controllerGenerator;
        this.appGenerator = appGenerator;
        this.exceptionGenerator = exceptionGenerator;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public GeneratedProject exportToZip(GeneratedJavaModel javaModel, byte[] flywaySql) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            String basePackage = "com.generated.app";
            String basePath = "generated-backend/";
            String srcMainJava = basePath + "src/main/java/com/generated/app/";

            // root files
            addEntry(zos, basePath + "pom.xml", pomXmlGenerator.generate());
            addEntry(zos, basePath + "README.md", "# Generated Backend\nRun with `./mvnw spring-boot:run`");
            
            // Maven Wrapper
            copyResourceToZip(zos, "classpath:maven-wrapper/mvnw", basePath + "mvnw");
            copyResourceToZip(zos, "classpath:maven-wrapper/mvnw.cmd", basePath + "mvnw.cmd");
            copyResourceToZip(zos, "classpath:maven-wrapper/.mvn/wrapper/maven-wrapper.properties", basePath + ".mvn/wrapper/maven-wrapper.properties");
            
            // Application
            addEntry(zos, srcMainJava + "GeneratedApplication.java", appGenerator.generate(basePackage));

            // Exception handlers
            addEntry(zos, srcMainJava + "exception/ResourceNotFoundException.java", exceptionGenerator.generateException(basePackage));
            addEntry(zos, srcMainJava + "exception/GlobalExceptionHandler.java", exceptionGenerator.generateGlobalHandler(basePackage));

            // Entities, Repositories, DTOs, Services, Controllers
            for (GeneratedJavaEntity entity : javaModel.entities()) {
                String className = entity.className();
                addEntry(zos, srcMainJava + "entity/" + className + ".java", entityGenerator.generate(entity, basePackage));
                addEntry(zos, srcMainJava + "repository/" + className + "Repository.java", repositoryGenerator.generate(entity, basePackage));
                addEntry(zos, srcMainJava + "dto/" + className + "Request.java", dtoGenerator.generateRequest(entity, basePackage));
                addEntry(zos, srcMainJava + "dto/" + className + "Response.java", dtoGenerator.generateResponse(entity, basePackage));
                addEntry(zos, srcMainJava + "service/" + className + "Service.java", serviceGenerator.generate(entity, basePackage));
                addEntry(zos, srcMainJava + "controller/" + className + "Controller.java", controllerGenerator.generate(entity, basePackage));
            }

            // Resources
            String resourcesPath = basePath + "src/main/resources/";
            String appYml = "spring:\n" +
                            "  datasource:\n" +
                            "    url: ${DB_URL:jdbc:postgresql://localhost:5432/generated_db}\n" +
                            "    username: ${DB_USERNAME:postgres}\n" +
                            "    password: ${DB_PASSWORD:postgres}\n" +
                            "  jpa:\n" +
                            "    hibernate:\n" +
                            "      ddl-auto: validate\n";
            addEntry(zos, resourcesPath + "application.yml", appYml);
            
            // Tests
            String testClass = "package com.generated.app;\n\n" +
                               "import org.junit.jupiter.api.Test;\n" +
                               "import org.springframework.boot.test.context.SpringBootTest;\n" +
                               "import org.springframework.test.context.DynamicPropertyRegistry;\n" +
                               "import org.springframework.test.context.DynamicPropertySource;\n\n" +
                               "@SpringBootTest\n" +
                               "class GeneratedApplicationTests {\n\n" +
                               "    @DynamicPropertySource\n" +
                               "    static void configureProperties(DynamicPropertyRegistry registry) {\n" +
                               "        registry.add(\"spring.datasource.url\", () -> \"jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH\");\n" +
                               "        registry.add(\"spring.datasource.username\", () -> \"sa\");\n" +
                               "        registry.add(\"spring.datasource.password\", () -> \"\");\n" +
                               "        registry.add(\"spring.datasource.driver-class-name\", () -> \"org.h2.Driver\");\n" +
                               "    }\n\n" +
                               "    @Test\n" +
                               "    void contextLoads() {\n" +
                               "    }\n" +
                               "}\n";
            addEntry(zos, basePath + "src/test/java/com/generated/app/GeneratedApplicationTests.java", testClass);

            // Flyway
            if (flywaySql != null && flywaySql.length > 0) {
                addEntry(zos, resourcesPath + "db/migration/V1__initial_schema.sql", flywaySql);
            } else {
                addEntry(zos, resourcesPath + "db/migration/V1__initial_schema.sql", "-- No tables\n");
            }

            zos.finish();
            return new GeneratedProject("generated-backend.zip", baos.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error generating ZIP", e);
        }
    }
    
    private void copyResourceToZip(ZipOutputStream zos, String resourcePath, String zipPath) throws IOException {
        Resource resource = resourceLoader.getResource(resourcePath);
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                byte[] content = FileCopyUtils.copyToByteArray(is);
                addEntry(zos, zipPath, content);
            }
        }
    }

    private void addEntry(ZipOutputStream zos, String path, String content) throws IOException {
        addEntry(zos, path, content.getBytes(StandardCharsets.UTF_8));
    }
    
    private void addEntry(ZipOutputStream zos, String path, byte[] content) throws IOException {
        // Zip slip protection for generation: Ensure path is clean
        if (path.startsWith("/") || path.contains("..") || path.contains(":\\")) {
            throw new IllegalArgumentException("Invalid path for zip entry: " + path);
        }
        
        ZipEntry entry = new ZipEntry(path);
        // Make it deterministic by setting constant time
        entry.setTime(0);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }
}
