package com.umlcase.application.handler.diagram;

import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.in.diagram.GetDiagramLayoutUseCase;
import com.umlcase.application.port.out.diagram.UmlDiagramLayoutRepository;
import com.umlcase.application.query.diagram.GetDiagramLayoutQuery;
import com.umlcase.domain.model.diagram.UmlDiagramLayout;
import org.springframework.stereotype.Service;

@Service
public class GetDiagramLayoutHandler implements GetDiagramLayoutUseCase {
    private final UmlDiagramLayoutRepository repository;

    public GetDiagramLayoutHandler(UmlDiagramLayoutRepository repository) {
        this.repository = repository;
    }

    @Override
    public UmlDiagramLayout execute(GetDiagramLayoutQuery query) {
        return repository.findByProjectId(query.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + query.getProjectId()));
    }
}
