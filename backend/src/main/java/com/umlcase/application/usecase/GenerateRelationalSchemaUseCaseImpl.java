package com.umlcase.application.usecase;

import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.mapper.relational.UmlToRelationalMapper;
import com.umlcase.application.port.in.GenerateRelationalSchemaUseCase;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.domain.relational.RelationalSchema;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GenerateRelationalSchemaUseCaseImpl implements GenerateRelationalSchemaUseCase {

    private final UmlModelRepository repository;
    private final UmlToRelationalMapper mapper;

    public GenerateRelationalSchemaUseCaseImpl(UmlModelRepository repository, UmlToRelationalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RelationalSchema generateSchema(UUID projectId) {
        UmlModel model = repository.findByProjectId(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Proyecto no encontrado: " + projectId));
        return mapper.mapToRelational(model);
    }
}
