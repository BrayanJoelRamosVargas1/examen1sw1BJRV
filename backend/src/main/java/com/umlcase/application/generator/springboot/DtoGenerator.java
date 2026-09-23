package com.umlcase.application.generator.springboot;

import com.umlcase.domain.generation.java.GeneratedJavaEntity;
import com.umlcase.domain.generation.java.GeneratedJavaField;
import org.springframework.stereotype.Component;

@Component
public class DtoGenerator {

    public String generateRequest(GeneratedJavaEntity entity, String basePackage) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".dto;\n\n");
        sb.append("import java.math.BigDecimal;\n");
        sb.append("import java.util.UUID;\n");
        sb.append("import java.util.List;\n\n");
        
        sb.append("public class ").append(entity.className()).append("Request {\n\n");
        
        for (GeneratedJavaField field : entity.fields()) {
            sb.append("    private ").append(field.type()).append(" ").append(field.name()).append(";\n");
        }
        sb.append("\n");
        
        for (GeneratedJavaField field : entity.fields()) {
            sb.append("    public ").append(field.type()).append(" get").append(capitalize(field.name())).append("() { return ").append(field.name()).append("; }\n");
            sb.append("    public void set").append(capitalize(field.name())).append("(").append(field.type()).append(" ").append(field.name()).append(") { this.").append(field.name()).append(" = ").append(field.name()).append("; }\n");
        }
        
        sb.append("}\n");
        return sb.toString();
    }
    
    public String generateResponse(GeneratedJavaEntity entity, String basePackage) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".dto;\n\n");
        sb.append("import java.math.BigDecimal;\n");
        sb.append("import java.util.UUID;\n");
        sb.append("import java.util.List;\n\n");
        
        sb.append("public class ").append(entity.className()).append("Response {\n\n");
        
        sb.append("    private UUID id;\n");
        for (GeneratedJavaField field : entity.fields()) {
            sb.append("    private ").append(field.type()).append(" ").append(field.name()).append(";\n");
        }
        sb.append("\n");
        
        sb.append("    public UUID getId() { return id; }\n");
        sb.append("    public void setId(UUID id) { this.id = id; }\n");
        for (GeneratedJavaField field : entity.fields()) {
            sb.append("    public ").append(field.type()).append(" get").append(capitalize(field.name())).append("() { return ").append(field.name()).append("; }\n");
            sb.append("    public void set").append(capitalize(field.name())).append("(").append(field.type()).append(" ").append(field.name()).append(") { this.").append(field.name()).append(" = ").append(field.name()).append("; }\n");
        }
        
        sb.append("}\n");
        return sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
