package com.umlcase.application.handler.diagram;

import com.umlcase.application.command.diagram.SaveNodeViewCommand;
import com.umlcase.domain.model.diagram.UmlDiagramLayout;
import com.umlcase.infrastructure.persistence.entity.JpaUmlClassEntity;
import com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity;
import com.umlcase.infrastructure.persistence.repository.SpringDataUmlModelRepository;
import com.umlcase.infrastructure.persistence.repository.diagram.JpaUmlDiagramLayoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SaveNodeViewTransactionalIT {

    @Autowired
    private SaveNodeViewHandler handler;

    @Autowired
    private SpringDataUmlModelRepository modelRepository;

    @Autowired
    private JpaUmlDiagramLayoutRepository layoutRepository;

    @BeforeEach
    void setup() {
        layoutRepository.deleteAll();
        modelRepository.deleteAll();
    }

    @Test
    void testMoveNodeDoesNotChangeModelVersion() {
        String projectId = UUID.randomUUID().toString();
        String classId = UUID.randomUUID().toString();

        JpaUmlModelEntity modelEntity = new JpaUmlModelEntity();
        modelEntity.setId(UUID.fromString(projectId));
        modelEntity.setProjectId(UUID.fromString(projectId));
        modelEntity.setVersion(0L);
        modelEntity.setLastModified(java.time.Instant.now().toEpochMilli());

        JpaUmlClassEntity classEntity = new JpaUmlClassEntity();
        classEntity.setId(UUID.fromString(classId));
        classEntity.setName("TestClass");
        modelEntity.getClasses().add(classEntity);

        modelRepository.saveAndFlush(modelEntity);

        com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity layoutEntity = new com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity();
        layoutEntity.setProjectId(UUID.fromString(projectId));
        layoutEntity.setVersion(0L);
        layoutEntity.setLastModified(java.time.Instant.now());
        layoutRepository.saveAndFlush(layoutEntity);

        long modelVersionBefore = modelRepository.findById(UUID.fromString(projectId)).get().getVersion();

        SaveNodeViewCommand command = SaveNodeViewCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .participantId("P1")
                .projectId(projectId)
                .classId(classId)
                .expectedLayoutVersion(0)
                .x(100)
                .y(200)
                .build();

        long newLayoutVersion = handler.execute(command);
        assertEquals(1, newLayoutVersion);

        long modelVersionAfter = modelRepository.findById(UUID.fromString(projectId)).get().getVersion();

        // THIS IS THE CRITICAL ASSERTION requested by the user: modelVersion remains unchanged
        assertEquals(modelVersionBefore, modelVersionAfter, "modelVersion debe permanecer intacta al modificar el layout");

        // Layout version should be 1
        assertEquals(1, layoutRepository.findById(UUID.fromString(projectId)).get().getVersion());
    }
}
