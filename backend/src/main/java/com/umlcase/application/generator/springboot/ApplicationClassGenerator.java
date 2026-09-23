package com.umlcase.application.generator.springboot;

import org.springframework.stereotype.Component;

@Component
public class ApplicationClassGenerator {

    public String generate(String basePackage) {
        return "package " + basePackage + ";\n\n" +
               "import org.springframework.boot.SpringApplication;\n" +
               "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
               "@SpringBootApplication\n" +
               "public class GeneratedApplication {\n\n" +
               "    public static void main(String[] args) {\n" +
               "        SpringApplication.run(GeneratedApplication.class, args);\n" +
               "    }\n" +
               "}\n";
    }
}
