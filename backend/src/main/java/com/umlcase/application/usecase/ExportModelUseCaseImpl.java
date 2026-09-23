package com.umlcase.application.usecase;

import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.in.ExportModelUseCase;
import com.umlcase.application.port.out.UmlInterchangeExporter;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ExportModelUseCaseImpl implements ExportModelUseCase {

    private final UmlModelRepository projectRepository;
    private final UmlInterchangeExporter exporter;

    public ExportModelUseCaseImpl(UmlModelRepository projectRepository, UmlInterchangeExporter exporter) {
        this.projectRepository = projectRepository;
        this.exporter = exporter;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportModel(UUID projectId) {
        UmlModel model = projectRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
        
        return exporter.export(model);
    }
}
