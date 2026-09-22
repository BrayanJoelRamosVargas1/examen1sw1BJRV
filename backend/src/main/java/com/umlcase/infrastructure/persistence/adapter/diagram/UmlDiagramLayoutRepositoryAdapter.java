package com.umlcase.infrastructure.persistence.adapter.diagram;

import com.umlcase.application.port.out.diagram.UmlDiagramLayoutRepository;
import com.umlcase.domain.exception.DiagramVersionConflictException;
import com.umlcase.domain.model.diagram.UmlDiagramLayout;
import com.umlcase.domain.model.diagram.UmlNodeView;
import com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity;
import com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlNodeViewEntity;
import com.umlcase.infrastructure.persistence.repository.diagram.JpaUmlDiagramLayoutRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UmlDiagramLayoutRepositoryAdapter implements UmlDiagramLayoutRepository {

    private final JpaUmlDiagramLayoutRepository jpaRepository;

    public UmlDiagramLayoutRepositoryAdapter(JpaUmlDiagramLayoutRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UmlDiagramLayout> findByProjectId(String projectId) {
        return jpaRepository.findById(java.util.UUID.fromString(projectId))
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public long save(UmlDiagramLayout layout, long expectedVersion) {
        JpaUmlDiagramLayoutEntity entity = jpaRepository.findById(java.util.UUID.fromString(layout.getProjectId()))
                .orElseGet(() -> {
                    JpaUmlDiagramLayoutEntity newEntity = new JpaUmlDiagramLayoutEntity();
                    newEntity.setProjectId(java.util.UUID.fromString(layout.getProjectId()));
                    newEntity.setVersion(0);
                    return newEntity;
                });

        if (entity.getVersion() != expectedVersion) {
            throw new DiagramVersionConflictException("Diagram layout modified concurrently. Expected: " 
                    + expectedVersion + ", actual: " + entity.getVersion());
        }

        entity.markModified();

        // Map NodeViews
        entity.getNodeViews().clear();
        for (UmlNodeView nv : layout.getNodeViews()) {
            JpaUmlNodeViewEntity jpaNv = new JpaUmlNodeViewEntity();
            jpaNv.setId(java.util.UUID.fromString(nv.getId()));
            jpaNv.setDiagramLayout(entity);
            jpaNv.setClassId(java.util.UUID.fromString(nv.getClassId()));
            jpaNv.setX(nv.getX());
            jpaNv.setY(nv.getY());
            entity.getNodeViews().add(jpaNv);
        }

        jpaRepository.saveAndFlush(entity);
        return entity.getVersion();
    }

    private UmlDiagramLayout toDomain(JpaUmlDiagramLayoutEntity entity) {
        return UmlDiagramLayout.builder()
                .projectId(entity.getProjectId().toString())
                .version(entity.getVersion())
                .nodeViews(entity.getNodeViews().stream()
                        .map(nv -> UmlNodeView.builder()
                                .id(nv.getId().toString())
                                .classId(nv.getClassId().toString())
                                .x(nv.getX())
                                .y(nv.getY())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
