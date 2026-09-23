package com.umlcase.application.mapper.relational;

import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.relational.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class UmlToRelationalMapper {

    private final SqlNamingStrategy namingStrategy;
    private final RelationalTypeMapper typeMapper;
    private final RelationshipRelationalMapper relationshipMapper;

    public UmlToRelationalMapper(SqlNamingStrategy namingStrategy,
                                 RelationalTypeMapper typeMapper,
                                 RelationshipRelationalMapper relationshipMapper) {
        this.namingStrategy = namingStrategy;
        this.typeMapper = typeMapper;
        this.relationshipMapper = relationshipMapper;
    }

    public RelationalSchema mapToRelational(UmlModel model) {
        RelationalSchema schema = new RelationalSchema();
        namingStrategy.reset();

        Map<UUID, RelationalTable> tableMap = new HashMap<>();

        // First pass: create tables for all classes
        for (UmlClass cls : model.getClasses()) {
            RelationalTable table = new RelationalTable(namingStrategy.toTableName(cls.getName()));
            namingStrategy.resetColumns();

            // Technical PK
            String pkName = namingStrategy.toColumnName("id");
            table.addColumn(new RelationalColumn(pkName, "UUID", false));
            table.setPrimaryKey(new RelationalPrimaryKey(List.of(pkName)));

            // Attributes to Columns
            for (UmlAttribute attr : cls.getAttributes()) {
                String colName = namingStrategy.toColumnName(attr.getName(), true);
                String sqlType = typeMapper.mapType(attr.getType());
                // UML attributes are nullable by default in this implementation unless multiplicity is 1, but we don't model attribute multiplicity yet
                table.addColumn(new RelationalColumn(colName, sqlType, true));
            }

            tableMap.put(cls.getId(), table);
            schema.addTable(table);
        }

        // Second pass: map relationships (Generalization, Association, Aggregation, Composition)
        relationshipMapper.mapRelationships(model, tableMap, schema);

        return schema;
    }
}
