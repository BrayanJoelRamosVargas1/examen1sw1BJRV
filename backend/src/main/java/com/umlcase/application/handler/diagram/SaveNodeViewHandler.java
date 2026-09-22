package com.umlcase.application.handler.diagram;

import com.umlcase.application.command.diagram.SaveNodeViewCommand;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.in.diagram.SaveNodeViewUseCase;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.application.port.out.diagram.UmlDiagramLayoutRepository;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.diagram.UmlDiagramLayout;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class SaveNodeViewHandler implements SaveNodeViewUseCase {
    private final UmlDiagramLayoutRepository diagramRepository;
    private final UmlModelRepository modelRepository;

    public SaveNodeViewHandler(UmlDiagramLayoutRepository diagramRepository, UmlModelRepository modelRepository) {
        this.diagramRepository = diagramRepository;
        this.modelRepository = modelRepository;
    }

    @Override
    public long execute(SaveNodeViewCommand command) {
        if (!Double.isFinite(command.getX()) || !Double.isFinite(command.getY())) {
            throw new IllegalArgumentException("Coordinates must be finite numbers");
        }

        UmlModel model = modelRepository.findByProjectId(java.util.UUID.fromString(command.getProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + command.getProjectId()));
                
        boolean classExists = model.getClasses().stream()
                .anyMatch(c -> c.getId().equals(java.util.UUID.fromString(command.getClassId())));
        if (!classExists) {
            throw new IllegalArgumentException("Class not found: " + command.getClassId());
        }

        UmlDiagramLayout layout = diagramRepository.findByProjectId(command.getProjectId())
                .orElseGet(() -> UmlDiagramLayout.builder()
                        .projectId(command.getProjectId())
                        .version(0)
                        .nodeViews(new ArrayList<>())
                        .build());

        layout.upsertNodeView(command.getClassId(), command.getX(), command.getY());

        // Increment version to return the new layoutVersion (save operation handles the persist and optimistic locking)
        return diagramRepository.save(layout, command.getExpectedLayoutVersion());
    }
}
