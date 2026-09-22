package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RemoveRelationshipRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RemoveRelationshipApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UmlModelRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private UmlModel model;
    private UmlRelationship relationship;

    @BeforeEach
    void setUp() {
        model = UmlModel.create(UUID.randomUUID());

        UmlClass class1 = new UmlClass(UUID.randomUUID(), "Class1");
        UmlClass class2 = new UmlClass(UUID.randomUUID(), "Class2");
        model.addClass(class1);
        model.addClass(class2);

        relationship = new UmlRelationship(
                UUID.randomUUID(),
                RelationshipType.ASSOCIATION,
                class1.getId(),
                class2.getId(),
                "1",
                "*"
        );
        model.addRelationship(relationship);
        model = repository.save(model);
    }

    @AfterEach
    void tearDown() {
        repository.deleteById(model.getId());
    }

    @Test
    void shouldRemoveRelationship() throws Exception {
        UUID commandId = UUID.randomUUID();
        RemoveRelationshipRequest request = new RemoveRelationshipRequest(
                commandId,
                "browser-A",
                model.getVersion()
        );

        mockMvc.perform(delete("/api/projects/{projectId}/relationships/{relationshipId}",
                        model.getProjectId(), relationship.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandId").value(commandId.toString()))
                .andExpect(jsonPath("$.relationshipId").value(relationship.getId().toString()))
                .andExpect(jsonPath("$.modelVersion").value(model.getVersion() + 1));

        UmlModel updatedModel = repository.findByProjectId(model.getProjectId()).orElseThrow();
        assertThat(updatedModel.getVersion()).isEqualTo(model.getVersion() + 1);
        assertThat(updatedModel.getRelationships()).isEmpty();
    }

    @Test
    void shouldReturn409OnVersionConflict() throws Exception {
        UUID commandId = UUID.randomUUID();
        RemoveRelationshipRequest request = new RemoveRelationshipRequest(
                commandId,
                "browser-A",
                model.getVersion() - 1 // stale
        );

        mockMvc.perform(delete("/api/projects/{projectId}/relationships/{relationshipId}",
                        model.getProjectId(), relationship.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        UmlModel updatedModel = repository.findByProjectId(model.getProjectId()).orElseThrow();
        assertThat(updatedModel.getVersion()).isEqualTo(model.getVersion());
        assertThat(updatedModel.getRelationships()).hasSize(1);
    }
}
