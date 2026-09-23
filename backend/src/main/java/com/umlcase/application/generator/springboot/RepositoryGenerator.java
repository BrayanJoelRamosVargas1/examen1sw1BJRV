package com.umlcase.application.generator.springboot;

import com.umlcase.domain.generation.java.GeneratedJavaEntity;
import org.springframework.stereotype.Component;

@Component
public class RepositoryGenerator {

    public String generate(GeneratedJavaEntity entity, String basePackage) {
        return "package " + basePackage + ".repository;\n\n" +
               "import " + basePackage + ".entity." + entity.className() + ";\n" +
               "import org.springframework.data.jpa.repository.JpaRepository;\n" +
               "import org.springframework.stereotype.Repository;\n" +
               "import java.util.UUID;\n\n" +
               "@Repository\n" +
               "public interface " + entity.className() + "Repository extends JpaRepository<" + entity.className() + ", UUID> {\n" +
               "}\n";
    }
}
