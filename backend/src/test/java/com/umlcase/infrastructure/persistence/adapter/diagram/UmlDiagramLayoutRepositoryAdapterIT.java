package com.umlcase.infrastructure.persistence.adapter.diagram;

import com.umlcase.domain.exception.DiagramVersionConflictException;
import com.umlcase.domain.model.diagram.UmlDiagramLayout;
import com.umlcase.domain.model.diagram.UmlNodeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UmlDiagramLayoutRepositoryAdapterIT {

    @Autowired
    private UmlDiagramLayoutRepositoryAdapter adapter;

    @Autowired
    private com.umlcase.infrastructure.persistence.repository.diagram.JpaUmlDiagramLayoutRepository jpaLayoutRepository;

    @Test
    @Transactional
    void testSaveAndFind() {
        String projectId = UUID.randomUUID().toString();
        
        com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity layoutEntity = new com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity();
        layoutEntity.setProjectId(UUID.fromString(projectId));
        layoutEntity.setVersion(0L);
        layoutEntity.setLastModified(java.time.Instant.now());
        jpaLayoutRepository.saveAndFlush(layoutEntity);
        
        UmlDiagramLayout layout = UmlDiagramLayout.builder()
                .projectId(projectId)
                .version(0)
                .nodeViews(new ArrayList<>())
                .build();
        
        layout.upsertNodeView(UUID.randomUUID().toString(), 100, 200);
        
        adapter.save(layout, 0);

        Optional<UmlDiagramLayout> found = adapter.findByProjectId(projectId);
        assertTrue(found.isPresent());
        assertEquals(projectId, found.get().getProjectId());
        assertEquals(1, found.get().getVersion());
        assertEquals(1, found.get().getNodeViews().size());
        assertEquals(100, found.get().getNodeViews().get(0).getX());
        assertEquals(200, found.get().getNodeViews().get(0).getY());
    }

    @Test
    @Transactional
    void testOptimisticLockingConflict() {
        String projectId = UUID.randomUUID().toString();

        com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity layoutEntity = new com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity();
        layoutEntity.setProjectId(UUID.fromString(projectId));
        layoutEntity.setVersion(0L);
        layoutEntity.setLastModified(java.time.Instant.now());
        jpaLayoutRepository.saveAndFlush(layoutEntity);

        UmlDiagramLayout layout = UmlDiagramLayout.builder()
                .projectId(projectId)
                .version(0)
                .nodeViews(new ArrayList<>())
                .build();

        adapter.save(layout, 0);

        // Try to save again with expectedVersion = 0, but it should be 1 now
        assertThrows(DiagramVersionConflictException.class, () -> adapter.save(layout, 0));
    }
}
