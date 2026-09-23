package com.umlcase.application.mapper.relational;

import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.relational.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RelationshipRelationalMapper {

    private final SqlNamingStrategy namingStrategy;

    public RelationshipRelationalMapper(SqlNamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public void mapRelationships(UmlModel model, Map<UUID, RelationalTable> tableMap, RelationalSchema schema) {
        // First map generalizations to avoid duplicating columns and setup joined inheritance
        checkGeneralizationCycles(model);
        
        List<UmlRelationship> generalizations = model.getRelationships().stream()
                .filter(r -> r.getType() == RelationshipType.GENERALIZATION)
                .toList();

        for (UmlRelationship gen : generalizations) {
            RelationalTable childTable = tableMap.get(gen.getSourceClassId());
            RelationalTable parentTable = tableMap.get(gen.getTargetClassId());
            
            if (childTable != null && parentTable != null) {
                // JOINED inheritance: child gets an FK to parent
                String fkName = "fk_" + childTable.getName() + "_" + parentTable.getName() + "_gen";
                childTable.addForeignKey(new RelationalForeignKey(
                        fkName,
                        List.of("id"),
                        parentTable.getName(),
                        List.of("id"),
                        true // ON DELETE CASCADE for inheritance
                ));
            }
        }

        // Map associations, aggregations, compositions
        List<UmlRelationship> others = model.getRelationships().stream()
                .filter(r -> r.getType() != RelationshipType.GENERALIZATION)
                .toList();

        for (UmlRelationship rel : others) {
            boolean isComposition = rel.getType() == RelationshipType.COMPOSITION;
            
            UmlClass source = model.getClasses().stream().filter(c -> c.getId().equals(rel.getSourceClassId())).findFirst().orElse(null);
            UmlClass target = model.getClasses().stream().filter(c -> c.getId().equals(rel.getTargetClassId())).findFirst().orElse(null);
            
            if (source == null || target == null) continue;
            
            RelationalTable sourceTable = tableMap.get(source.getId());
            RelationalTable targetTable = tableMap.get(target.getId());

            boolean sourceIsMany = isMany(rel.getSourceMultiplicity());
            boolean targetIsMany = isMany(rel.getTargetMultiplicity());
            boolean sourceIsOptional = isOptional(rel.getSourceMultiplicity());
            boolean targetIsOptional = isOptional(rel.getTargetMultiplicity());

            if (sourceIsMany && targetIsMany) {
                // N-M -> Join table
                String joinTableName = generateJoinTableName(sourceTable.getName(), targetTable.getName());
                RelationalTable joinTable = new RelationalTable(namingStrategy.toTableName(joinTableName));
                
                String sourceFkCol = namingStrategy.toSnakeCase(sourceTable.getName()) + "_id";
                String targetFkCol = namingStrategy.toSnakeCase(targetTable.getName()) + "_id";
                
                // If same table, distinguish columns
                if (sourceFkCol.equals(targetFkCol)) {
                    sourceFkCol = "source_" + sourceFkCol;
                    targetFkCol = "target_" + targetFkCol;
                }
                
                joinTable.addColumn(new RelationalColumn(sourceFkCol, "UUID", false));
                joinTable.addColumn(new RelationalColumn(targetFkCol, "UUID", false));
                joinTable.setPrimaryKey(new RelationalPrimaryKey(List.of(sourceFkCol, targetFkCol)));
                
                joinTable.addForeignKey(new RelationalForeignKey(
                        "fk_" + joinTable.getName() + "_" + sourceTable.getName(),
                        List.of(sourceFkCol), sourceTable.getName(), List.of("id"), isComposition
                ));
                joinTable.addForeignKey(new RelationalForeignKey(
                        "fk_" + joinTable.getName() + "_" + targetTable.getName(),
                        List.of(targetFkCol), targetTable.getName(), List.of("id"), isComposition
                ));
                
                schema.addTable(joinTable);
            } else if (sourceIsMany && !targetIsMany) {
                // N-1 -> FK on Source table
                addForeignKey(sourceTable, targetTable, targetIsOptional, isComposition);
            } else if (!sourceIsMany && targetIsMany) {
                // 1-N -> FK on Target table
                addForeignKey(targetTable, sourceTable, sourceIsOptional, isComposition);
            } else {
                // 1-1
                if (sourceIsOptional && !targetIsOptional) {
                    addForeignKeyWithUnique(sourceTable, targetTable, true, isComposition);
                } else if (!sourceIsOptional && targetIsOptional) {
                    addForeignKeyWithUnique(targetTable, sourceTable, true, isComposition);
                } else {
                    addForeignKeyWithUnique(targetTable, sourceTable, targetIsOptional, isComposition);
                }
            }
        }
    }

    private void checkGeneralizationCycles(UmlModel model) {
        Map<UUID, List<UUID>> graph = new HashMap<>();
        for (UmlRelationship r : model.getRelationships()) {
            if (r.getType() == RelationshipType.GENERALIZATION) {
                graph.computeIfAbsent(r.getSourceClassId(), k -> new ArrayList<>()).add(r.getTargetClassId());
            }
        }

        Set<UUID> visited = new HashSet<>();
        Set<UUID> recStack = new HashSet<>();

        for (UUID node : graph.keySet()) {
            if (isCyclic(node, graph, visited, recStack)) {
                throw new IllegalArgumentException("Ciclo de herencia detectado");
            }
        }
    }

    private boolean isCyclic(UUID node, Map<UUID, List<UUID>> graph, Set<UUID> visited, Set<UUID> recStack) {
        if (recStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        recStack.add(node);

        for (UUID neighbor : graph.getOrDefault(node, Collections.emptyList())) {
            if (isCyclic(neighbor, graph, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(node);
        return false;
    }

    private boolean isMany(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") || multiplicity.contains("n");
    }

    private boolean isOptional(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.startsWith("0");
    }

    private String generateJoinTableName(String t1, String t2) {
        if (t1.compareTo(t2) <= 0) {
            return t1 + "_" + t2;
        } else {
            return t2 + "_" + t1;
        }
    }

    private void addForeignKey(RelationalTable from, RelationalTable to, boolean isNullable, boolean isComposition) {
        String colName = namingStrategy.toSnakeCase(to.getName()) + "_id";
        
        // Handle self-referencing or multiple FKs to same table by renaming column
        int i = 1;
        String baseColName = colName;
        while (hasColumn(from, colName)) {
            colName = baseColName + "_" + i;
            i++;
        }
        
        from.addColumn(new RelationalColumn(colName, "UUID", isNullable));
        from.addForeignKey(new RelationalForeignKey(
                "fk_" + from.getName() + "_" + colName,
                List.of(colName),
                to.getName(),
                List.of("id"),
                isComposition
        ));
    }

    private void addForeignKeyWithUnique(RelationalTable from, RelationalTable to, boolean isNullable, boolean isComposition) {
        String colName = namingStrategy.toSnakeCase(to.getName()) + "_id";
        
        int i = 1;
        String baseColName = colName;
        while (hasColumn(from, colName)) {
            colName = baseColName + "_" + i;
            i++;
        }
        
        from.addColumn(new RelationalColumn(colName, "UUID", isNullable));
        from.addForeignKey(new RelationalForeignKey(
                "fk_" + from.getName() + "_" + colName,
                List.of(colName),
                to.getName(),
                List.of("id"),
                isComposition
        ));
        from.addUniqueConstraint(new RelationalUniqueConstraint(
                "uq_" + from.getName() + "_" + colName,
                List.of(colName)
        ));
    }

    private boolean hasColumn(RelationalTable table, String colName) {
        return table.getColumns().stream().anyMatch(c -> c.name().equals(colName));
    }
}
