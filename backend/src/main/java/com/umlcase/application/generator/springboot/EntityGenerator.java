package com.umlcase.application.generator.springboot;

import com.umlcase.domain.generation.java.GeneratedJavaEntity;
import com.umlcase.domain.generation.java.GeneratedJavaField;
import com.umlcase.domain.generation.java.GeneratedJavaRelationship;
import org.springframework.stereotype.Component;

@Component
public class EntityGenerator {

    public String generate(GeneratedJavaEntity entity, String basePackage) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".entity;\n\n");
        sb.append("import jakarta.persistence.*;\n");
        sb.append("import java.util.*;\n");
        sb.append("import java.math.BigDecimal;\n\n");

        sb.append("@Entity\n");
        if (entity.parentClassName() == null) {
            // Only define table name for root entities or those not using joined inheritance subclasses
            sb.append("@Table(name = \"").append(entity.tableName()).append("\")\n");
        }
        
        if (entity.isJoinedInheritanceRoot()) {
            sb.append("@Inheritance(strategy = InheritanceType.JOINED)\n");
        }
        
        sb.append("public class ").append(entity.className());
        if (entity.parentClassName() != null) {
            sb.append(" extends ").append(entity.parentClassName());
        }
        sb.append(" {\n\n");

        if (entity.parentClassName() == null) {
            sb.append("    @Id\n");
            sb.append("    @GeneratedValue(strategy = GenerationType.UUID)\n");
            sb.append("    private UUID ").append(entity.primaryKeyName()).append(";\n\n");
        }

        for (GeneratedJavaField field : entity.fields()) {
            sb.append("    @Column(name = \"").append(field.columnName()).append("\"");
            if (!field.isNullable()) {
                sb.append(", nullable = false");
            }
            sb.append(")\n");
            sb.append("    private ").append(field.type()).append(" ").append(field.name()).append(";\n\n");
        }

        for (GeneratedJavaRelationship rel : entity.relationships()) {
            sb.append("    ").append(rel.relationshipType());
            
            boolean hasAttrs = false;
            if (rel.mappedBy() != null || rel.isComposition()) {
                sb.append("(");
                if (rel.mappedBy() != null) {
                    sb.append("mappedBy = \"").append(rel.mappedBy()).append("\"");
                    hasAttrs = true;
                }
                if (rel.isComposition()) {
                    if (hasAttrs) sb.append(", ");
                    sb.append("cascade = CascadeType.ALL, orphanRemoval = true");
                }
                sb.append(")");
            }
            sb.append("\n");
            
            if (rel.joinColumnName() != null) {
                sb.append("    @JoinColumn(name = \"").append(rel.joinColumnName()).append("\")\n");
            }
            if (rel.joinTableName() != null) {
                sb.append("    @JoinTable(name = \"").append(rel.joinTableName()).append("\",\n");
                sb.append("        joinColumns = @JoinColumn(name = \"").append(rel.joinColumnName()).append("\"),\n");
                sb.append("        inverseJoinColumns = @JoinColumn(name = \"").append(rel.inverseJoinColumnName()).append("\")\n");
                sb.append("    )\n");
            }
            
            String javaType = rel.targetClassName();
            if (rel.relationshipType().equals("@OneToMany") || rel.relationshipType().equals("@ManyToMany")) {
                javaType = "List<" + javaType + ">";
                sb.append("    private ").append(javaType).append(" ").append(rel.fieldName()).append(" = new ArrayList<>();\n\n");
            } else {
                sb.append("    private ").append(javaType).append(" ").append(rel.fieldName()).append(";\n\n");
            }
        }

        // Getters and Setters
        if (entity.parentClassName() == null) {
            sb.append("    public UUID get").append(capitalize(entity.primaryKeyName())).append("() { return ").append(entity.primaryKeyName()).append("; }\n");
            sb.append("    public void set").append(capitalize(entity.primaryKeyName())).append("(UUID ").append(entity.primaryKeyName()).append(") { this.").append(entity.primaryKeyName()).append(" = ").append(entity.primaryKeyName()).append("; }\n\n");
        }

        for (GeneratedJavaField field : entity.fields()) {
            sb.append("    public ").append(field.type()).append(" get").append(capitalize(field.name())).append("() { return ").append(field.name()).append("; }\n");
            sb.append("    public void set").append(capitalize(field.name())).append("(").append(field.type()).append(" ").append(field.name()).append(") { this.").append(field.name()).append(" = ").append(field.name()).append("; }\n\n");
        }
        
        for (GeneratedJavaRelationship rel : entity.relationships()) {
            String javaType = rel.targetClassName();
            if (rel.relationshipType().equals("@OneToMany") || rel.relationshipType().equals("@ManyToMany")) {
                javaType = "List<" + javaType + ">";
            }
            sb.append("    public ").append(javaType).append(" get").append(capitalize(rel.fieldName())).append("() { return ").append(rel.fieldName()).append("; }\n");
            sb.append("    public void set").append(capitalize(rel.fieldName())).append("(").append(javaType).append(" ").append(rel.fieldName()).append(") { this.").append(rel.fieldName()).append(" = ").append(rel.fieldName()).append("; }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
