package com.umlcase.application.generator.springboot;

import com.umlcase.domain.generation.java.GeneratedJavaEntity;
import org.springframework.stereotype.Component;

@Component
public class ControllerGenerator {

    public String generate(GeneratedJavaEntity entity, String basePackage) {
        String entityName = entity.className();
        String svcName = entityName + "Service";
        String svcVar = Character.toLowerCase(svcName.charAt(0)) + svcName.substring(1);
        String reqName = entityName + "Request";
        String resName = entityName + "Response";
        String basePath = "/api/" + entity.tableName().replace("_", "-");
        
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".controller;\n\n");
        
        sb.append("import ").append(basePackage).append(".dto.").append(reqName).append(";\n");
        sb.append("import ").append(basePackage).append(".dto.").append(resName).append(";\n");
        sb.append("import ").append(basePackage).append(".service.").append(svcName).append(";\n");
        sb.append("import org.springframework.http.HttpStatus;\n");
        sb.append("import org.springframework.web.bind.annotation.*;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.UUID;\n\n");
        
        sb.append("@RestController\n");
        sb.append("@RequestMapping(\"").append(basePath).append("\")\n");
        sb.append("public class ").append(entityName).append("Controller {\n\n");
        
        sb.append("    private final ").append(svcName).append(" ").append(svcVar).append(";\n\n");
        
        sb.append("    public ").append(entityName).append("Controller(").append(svcName).append(" ").append(svcVar).append(") {\n");
        sb.append("        this.").append(svcVar).append(" = ").append(svcVar).append(";\n");
        sb.append("    }\n\n");
        
        sb.append("    @PostMapping\n");
        sb.append("    @ResponseStatus(HttpStatus.CREATED)\n");
        sb.append("    public ").append(resName).append(" create(@RequestBody ").append(reqName).append(" request) {\n");
        sb.append("        return ").append(svcVar).append(".create(request);\n");
        sb.append("    }\n\n");
        
        sb.append("    @GetMapping\n");
        sb.append("    public List<").append(resName).append("> findAll() {\n");
        sb.append("        return ").append(svcVar).append(".findAll();\n");
        sb.append("    }\n\n");
        
        sb.append("    @GetMapping(\"/{id}\")\n");
        sb.append("    public ").append(resName).append(" findById(@PathVariable UUID id) {\n");
        sb.append("        return ").append(svcVar).append(".findById(id);\n");
        sb.append("    }\n\n");
        
        sb.append("    @PutMapping(\"/{id}\")\n");
        sb.append("    public ").append(resName).append(" update(@PathVariable UUID id, @RequestBody ").append(reqName).append(" request) {\n");
        sb.append("        return ").append(svcVar).append(".update(id, request);\n");
        sb.append("    }\n\n");
        
        sb.append("    @DeleteMapping(\"/{id}\")\n");
        sb.append("    @ResponseStatus(HttpStatus.NO_CONTENT)\n");
        sb.append("    public void delete(@PathVariable UUID id) {\n");
        sb.append("        ").append(svcVar).append(".delete(id);\n");
        sb.append("    }\n");
        
        sb.append("}\n");
        return sb.toString();
    }
}
