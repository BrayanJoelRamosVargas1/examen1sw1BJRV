package com.umlcase.infrastructure.persistence.repository.diagram;

import com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUmlDiagramLayoutRepository extends JpaRepository<JpaUmlDiagramLayoutEntity, java.util.UUID> {
}
