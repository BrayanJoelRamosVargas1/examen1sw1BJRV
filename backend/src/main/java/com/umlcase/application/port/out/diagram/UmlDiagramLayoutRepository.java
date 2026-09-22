package com.umlcase.application.port.out.diagram;

import com.umlcase.domain.model.diagram.UmlDiagramLayout;

import java.util.Optional;

public interface UmlDiagramLayoutRepository {
    Optional<UmlDiagramLayout> findByProjectId(String projectId);
    long save(UmlDiagramLayout layout, long expectedVersion);
}
