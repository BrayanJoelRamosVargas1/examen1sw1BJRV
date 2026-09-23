package com.umlcase.application.generator.springboot;

import com.umlcase.domain.generation.java.GeneratedJavaEntity;
import com.umlcase.domain.generation.java.GeneratedJavaField;
import org.springframework.stereotype.Component;

@Component
public class ServiceGenerator {

    public String generate(GeneratedJavaEntity entity, String basePackage) {
        String entityName = entity.className();
        String repoName = entityName + "Repository";
        String repoVar = Character.toLowerCase(repoName.charAt(0)) + repoName.substring(1);
        String reqName = entityName + "Request";
        String resName = entityName + "Response";
        
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".service;\n\n");
        
        sb.append("import ").append(basePackage).append(".entity.").append(entityName).append(";\n");
        sb.append("import ").append(basePackage).append(".repository.").append(repoName).append(";\n");
        sb.append("import ").append(basePackage).append(".dto.").append(reqName).append(";\n");
        sb.append("import ").append(basePackage).append(".dto.").append(resName).append(";\n");
        sb.append("import ").append(basePackage).append(".exception.ResourceNotFoundException;\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import org.springframework.transaction.annotation.Transactional;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.UUID;\n");
        sb.append("import java.util.stream.Collectors;\n\n");
        
        sb.append("@Service\n");
        sb.append("@Transactional\n");
        sb.append("public class ").append(entityName).append("Service {\n\n");
        
        sb.append("    private final ").append(repoName).append(" ").append(repoVar).append(";\n\n");
        
        sb.append("    public ").append(entityName).append("Service(").append(repoName).append(" ").append(repoVar).append(") {\n");
        sb.append("        this.").append(repoVar).append(" = ").append(repoVar).append(";\n");
        sb.append("    }\n\n");
        
        // create
        sb.append("    public ").append(resName).append(" create(").append(reqName).append(" request) {\n");
        sb.append("        ").append(entityName).append(" entity = new ").append(entityName).append("();\n");
        for (GeneratedJavaField field : entity.fields()) {
            sb.append("        entity.set").append(capitalize(field.name())).append("(request.get").append(capitalize(field.name())).append("());\n");
        }
        sb.append("        ").append(repoVar).append(".save(entity);\n");
        sb.append("        return mapToResponse(entity);\n");
        sb.append("    }\n\n");
        
        // findAll
        sb.append("    @Transactional(readOnly = true)\n");
        sb.append("    public List<").append(resName).append("> findAll() {\n");
        sb.append("        return ").append(repoVar).append(".findAll().stream().map(this::mapToResponse).collect(Collectors.toList());\n");
        sb.append("    }\n\n");
        
        // findById
        sb.append("    @Transactional(readOnly = true)\n");
        sb.append("    public ").append(resName).append(" findById(UUID id) {\n");
        sb.append("        ").append(entityName).append(" entity = ").append(repoVar).append(".findById(id)\n");
        sb.append("            .orElseThrow(() -> new ResourceNotFoundException(\"").append(entityName).append(" not found\"));\n");
        sb.append("        return mapToResponse(entity);\n");
        sb.append("    }\n\n");
        
        // update
        sb.append("    public ").append(resName).append(" update(UUID id, ").append(reqName).append(" request) {\n");
        sb.append("        ").append(entityName).append(" entity = ").append(repoVar).append(".findById(id)\n");
        sb.append("            .orElseThrow(() -> new ResourceNotFoundException(\"").append(entityName).append(" not found\"));\n");
        for (GeneratedJavaField field : entity.fields()) {
            sb.append("        entity.set").append(capitalize(field.name())).append("(request.get").append(capitalize(field.name())).append("());\n");
        }
        sb.append("        ").append(repoVar).append(".save(entity);\n");
        sb.append("        return mapToResponse(entity);\n");
        sb.append("    }\n\n");
        
        // delete
        sb.append("    public void delete(UUID id) {\n");
        sb.append("        if (!").append(repoVar).append(".existsById(id)) {\n");
        sb.append("            throw new ResourceNotFoundException(\"").append(entityName).append(" not found\");\n");
        sb.append("        }\n");
        sb.append("        ").append(repoVar).append(".deleteById(id);\n");
        sb.append("    }\n\n");
        
        // mapper
        sb.append("    private ").append(resName).append(" mapToResponse(").append(entityName).append(" entity) {\n");
        sb.append("        ").append(resName).append(" res = new ").append(resName).append("();\n");
        sb.append("        res.setId(entity.get").append(capitalize(entity.primaryKeyName())).append("());\n");
        for (GeneratedJavaField field : entity.fields()) {
            sb.append("        res.set").append(capitalize(field.name())).append("(entity.get").append(capitalize(field.name())).append("());\n");
        }
        sb.append("        return res;\n");
        sb.append("    }\n");
        
        sb.append("}\n");
        return sb.toString();
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
