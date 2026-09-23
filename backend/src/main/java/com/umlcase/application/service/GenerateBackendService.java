package com.umlcase.application.service;

import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.mapper.java.UmlToJavaModelMapper;
import com.umlcase.application.mapper.relational.UmlToRelationalMapper;
import com.umlcase.application.port.in.GenerateBackendUseCase;
import com.umlcase.application.port.in.GeneratedProject;
import com.umlcase.application.port.out.RelationalSchemaExporter;
import com.umlcase.application.port.out.SpringBootProjectExporter;
import com.umlcase.domain.generation.java.GeneratedJavaModel;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.domain.relational.RelationalSchema;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GenerateBackendService implements GenerateBackendUseCase {

    private final UmlModelRepository modelRepository;
    private final UmlToRelationalMapper relationalMapper;
    private final UmlToJavaModelMapper javaMapper;
    private final RelationalSchemaExporter sqlExporter;
    private final SpringBootProjectExporter zipExporter;

    public GenerateBackendService(UmlModelRepository modelRepository,
                                  UmlToRelationalMapper relationalMapper,
                                  UmlToJavaModelMapper javaMapper,
                                  RelationalSchemaExporter sqlExporter,
                                  SpringBootProjectExporter zipExporter) {
        this.modelRepository = modelRepository;
        this.relationalMapper = relationalMapper;
        this.javaMapper = javaMapper;
        this.sqlExporter = sqlExporter;
        this.zipExporter = zipExporter;
    }

    @Override
    public GeneratedProject generate(UUID projectId) {
        UmlModel model = modelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // 1. Map to Relational and generate Flyway SQL
        RelationalSchema schema = relationalMapper.mapToRelational(model);
        byte[] flywaySql = sqlExporter.exportToSql(schema);

        // 2. Map to Java Model
        GeneratedJavaModel javaModel = javaMapper.mapToJavaModel(model);

        // 3. Generate Zip
        return zipExporter.exportToZip(javaModel, flywaySql);
    }
}
