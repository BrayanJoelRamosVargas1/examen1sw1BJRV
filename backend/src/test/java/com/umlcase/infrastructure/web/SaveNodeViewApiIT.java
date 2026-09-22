package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.infrastructure.persistence.repository.SpringDataUmlModelRepository;
import com.umlcase.infrastructure.persistence.repository.diagram.JpaUmlDiagramLayoutRepository;
import com.umlcase.infrastructure.web.dto.diagram.SaveNodeViewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SaveNodeViewApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void testSaveNodeViewAndGetDiagram() throws Exception {
        // Create project and class
        String projectId = "00000000-0000-0000-0000-000000000001";
        String classId = "dbd46dc0-76be-45b5-ab5e-b38e6ab52de9";

        // Remove any previous node views from db for this class to ensure clean state
        // The project layout is already inserted by V5 migration with version 0

        com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity model = new com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity();
        model.setId(UUID.fromString(projectId));
        model.setProjectId(UUID.fromString(projectId));
        model.setVersion(0L);
        model.setLastModified(java.time.Instant.now().toEpochMilli());

        com.umlcase.infrastructure.persistence.entity.JpaUmlClassEntity cls = new com.umlcase.infrastructure.persistence.entity.JpaUmlClassEntity();
        cls.setId(UUID.fromString(classId));
        cls.setName("TestClass");
        model.getClasses().add(cls);
        modelRepository.saveAndFlush(model);

        com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity layoutEntity = new com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity();
        layoutEntity.setProjectId(UUID.fromString(projectId));
        layoutEntity.setVersion(0L);
        layoutEntity.setLastModified(java.time.Instant.now());
        layoutRepository.saveAndFlush(layoutEntity);

        SaveNodeViewRequest request = new SaveNodeViewRequest();
        request.setCommandId(UUID.randomUUID().toString());
        request.setParticipantId("test-client");
        request.setExpectedLayoutVersion(0);
        request.setX(100);
        request.setY(200);

        mockMvc.perform(put("/api/projects/{projectId}/diagram/nodes/{classId}", projectId, classId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(classId))
                .andExpect(jsonPath("$.x").value(100.0))
                .andExpect(jsonPath("$.y").value(200.0))
                .andExpect(jsonPath("$.layoutVersion").value(1));

        // Test GET
        mockMvc.perform(get("/api/projects/{projectId}/diagram", projectId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.layoutVersion").value(1))
                .andExpect(jsonPath("$.nodeViews[0].classId").value(classId))
                .andExpect(jsonPath("$.nodeViews[0].x").value(100.0))
                .andExpect(jsonPath("$.nodeViews[0].y").value(200.0));
    }
}
