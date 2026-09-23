package com.umlcase.application.mapper.java;

import com.umlcase.domain.generation.java.*;
import com.umlcase.domain.model.*;
import com.umlcase.application.mapper.relational.SqlNamingStrategy;
import com.umlcase.application.mapper.relational.RelationalMappingContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UmlToJavaModelMapper {

    private final JavaNamingStrategy javaNaming;
    private final SqlNamingStrategy sqlNaming;

    public UmlToJavaModelMapper(JavaNamingStrategy javaNaming, SqlNamingStrategy sqlNaming) {
        this.javaNaming = javaNaming;
        this.sqlNaming = sqlNaming;
    }

    public GeneratedJavaModel mapToJavaModel(UmlModel model) {
        List<GeneratedJavaEntity> entities = new ArrayList<>();
        RelationalMappingContext sqlContext = new RelationalMappingContext();
        
        Map<UUID, GeneratedJavaEntity> entityMap = new HashMap<>();
        
        // Pass 1: Create basic entities with fields
        for (UmlClass cls : model.getClasses()) {
            String className = javaNaming.toClassName(cls.getName());
            String tableName = sqlContext.generateTableName(cls.getName(), sqlNaming);
            sqlContext.resetColumns();
            
            String pkColName = sqlContext.generateColumnName("id", sqlNaming, true);
            
            // Check inheritance parent
            String parentClassName = null;
            boolean isJoinedRoot = false;
            for (UmlRelationship rel : model.getRelationships()) {
                if (rel.getType() == RelationshipType.GENERALIZATION) {
                    if (rel.getSourceClassId().equals(cls.getId())) {
                        UmlClass parent = findClass(model, rel.getTargetClassId());
                        parentClassName = javaNaming.toClassName(parent.getName());
                    } else if (rel.getTargetClassId().equals(cls.getId())) {
                        isJoinedRoot = true;
                    }
                }
            }

            List<GeneratedJavaField> fields = new ArrayList<>();
            for (UmlAttribute attr : cls.getAttributes()) {
                String fieldName = javaNaming.toFieldName(attr.getName());
                String colName = sqlContext.generateColumnName(attr.getName(), sqlNaming, true);
                String javaType = mapJavaType(attr.getType());
                fields.add(new GeneratedJavaField(fieldName, javaType, colName, true));
            }
            
            GeneratedJavaEntity entity = new GeneratedJavaEntity(
                cls.getId(), className, tableName, "id", "UUID",
                parentClassName, isJoinedRoot, fields, new ArrayList<>()
            );
            entities.add(entity);
            entityMap.put(cls.getId(), entity);
        }
        
        // Pass 2: Map relationships
        for (UmlRelationship rel : model.getRelationships()) {
            if (rel.getType() == RelationshipType.GENERALIZATION) continue;
            
            GeneratedJavaEntity source = entityMap.get(rel.getSourceClassId());
            GeneratedJavaEntity target = entityMap.get(rel.getTargetClassId());
            
            boolean sourceIsMany = isMany(rel.getSourceMultiplicity());
            boolean targetIsMany = isMany(rel.getTargetMultiplicity());
            boolean isComposition = rel.getType() == RelationshipType.COMPOSITION;
            
            if (sourceIsMany && targetIsMany) {
                // N:M
                String fieldNameSource = javaNaming.toFieldName(target.className() + "s");
                String fieldNameTarget = javaNaming.toFieldName(source.className() + "s");
                
                String joinTableName = generateJoinTableName(source.tableName(), target.tableName());
                String joinTableNameSql = sqlContext.generateTableName(joinTableName, sqlNaming);
                
                String sourceFkCol = sqlNaming.toSnakeCase(source.tableName()) + "_id";
                String targetFkCol = sqlNaming.toSnakeCase(target.tableName()) + "_id";
                if (sourceFkCol.equals(targetFkCol)) {
                    sourceFkCol = "source_" + sourceFkCol;
                    targetFkCol = "target_" + targetFkCol;
                }
                
                source.relationships().add(new GeneratedJavaRelationship(
                    fieldNameSource, target.className(), "@ManyToMany", null,
                    null, joinTableNameSql, targetFkCol, isComposition
                ));
                target.relationships().add(new GeneratedJavaRelationship(
                    fieldNameTarget, source.className(), "@ManyToMany", fieldNameSource,
                    null, null, null, false
                ));
            } else if (sourceIsMany && !targetIsMany) {
                // N:1 -> Source has Many, Target has 1. FK goes to Source.
                String fieldNameSource = javaNaming.toFieldName(target.className());
                String fieldNameTarget = javaNaming.toFieldName(source.className() + "s");
                String fkCol = sqlNaming.toSnakeCase(target.tableName()) + "_id";
                
                source.relationships().add(new GeneratedJavaRelationship(
                    fieldNameSource, target.className(), "@ManyToOne", null,
                    fkCol, null, null, false
                ));
                target.relationships().add(new GeneratedJavaRelationship(
                    fieldNameTarget, source.className(), "@OneToMany", fieldNameSource,
                    null, null, null, isComposition
                ));
            } else if (!sourceIsMany && targetIsMany) {
                // 1:N -> Source has 1, Target has Many. FK goes to Target.
                String fieldNameSource = javaNaming.toFieldName(target.className() + "s");
                String fieldNameTarget = javaNaming.toFieldName(source.className());
                String fkCol = sqlNaming.toSnakeCase(source.tableName()) + "_id";
                
                source.relationships().add(new GeneratedJavaRelationship(
                    fieldNameSource, target.className(), "@OneToMany", fieldNameTarget,
                    null, null, null, isComposition
                ));
                target.relationships().add(new GeneratedJavaRelationship(
                    fieldNameTarget, source.className(), "@ManyToOne", null,
                    fkCol, null, null, false
                ));
            } else {
                // 1:1
                boolean targetOptional = isOptional(rel.getTargetMultiplicity());
                boolean sourceOptional = isOptional(rel.getSourceMultiplicity());
                
                String fieldNameSource = javaNaming.toFieldName(target.className());
                String fieldNameTarget = javaNaming.toFieldName(source.className());
                
                if (sourceOptional && !targetOptional) {
                    // FK on source
                    String fkCol = sqlNaming.toSnakeCase(target.tableName()) + "_id";
                    source.relationships().add(new GeneratedJavaRelationship(
                        fieldNameSource, target.className(), "@OneToOne", null,
                        fkCol, null, null, isComposition
                    ));
                    target.relationships().add(new GeneratedJavaRelationship(
                        fieldNameTarget, source.className(), "@OneToOne", fieldNameSource,
                        null, null, null, false
                    ));
                } else {
                    // FK on target
                    String fkCol = sqlNaming.toSnakeCase(source.tableName()) + "_id";
                    target.relationships().add(new GeneratedJavaRelationship(
                        fieldNameTarget, source.className(), "@OneToOne", null,
                        fkCol, null, null, false // Target is not composition owner usually
                    ));
                    source.relationships().add(new GeneratedJavaRelationship(
                        fieldNameSource, target.className(), "@OneToOne", fieldNameTarget,
                        null, null, null, isComposition
                    ));
                }
            }
        }
        
        entities.sort(Comparator.comparing(GeneratedJavaEntity::className));
        return new GeneratedJavaModel(entities);
    }
    
    private String mapJavaType(String umlType) {
        if (umlType == null) return "String";
        return switch (umlType.toLowerCase()) {
            case "integer", "int" -> "Integer";
            case "decimal", "float", "double" -> "java.math.BigDecimal";
            case "boolean" -> "Boolean";
            default -> "String";
        };
    }
    
    private UmlClass findClass(UmlModel model, UUID classId) {
        return model.getClasses().stream().filter(c -> c.getId().equals(classId)).findFirst().orElseThrow();
    }
    
    private boolean isMany(String mult) {
        if (mult == null) return false;
        return mult.contains("*") || mult.contains("N") || mult.contains("M");
    }
    
    private boolean isOptional(String mult) {
        if (mult == null) return true;
        return mult.startsWith("0..");
    }
    
    private String generateJoinTableName(String t1, String t2) {
        if (t1.compareTo(t2) <= 0) {
            return t1 + "_" + t2;
        } else {
            return t2 + "_" + t1;
        }
    }
}
