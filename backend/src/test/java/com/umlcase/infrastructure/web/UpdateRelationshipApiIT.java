package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.UpdateRelationshipRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UpdateRelationshipApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UmlModelRepository repository;

    @Test
    void put_updateRelationship_returns200AndModelVersion() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazzA = UmlClass.create("ClassA");
        UmlClass clazzB = UmlClass.create("ClassB");
        model.addClass(clazzA);
        model.addClass(clazzB);
        UmlRelationship rel = UmlRelationship.create(RelationshipType.ASSOCIATION, clazzA.getId(), clazzB.getId(), "1", "1");
        model.addRelationship(rel);
        UmlModel savedModel = repository.save(model);

        UpdateRelationshipRequest req = new UpdateRelationshipRequest(
                UUID.randomUUID(),
                "browser-1",
                savedModel.getVersion(),
                RelationshipType.AGGREGATION,
                "0..1",
                "*"
        );

        mockMvc.perform(put("/api/projects/" + projectId + "/relationships/" + rel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelVersion").value(savedModel.getVersion() + 1))
                .andExpect(jsonPath("$.relationshipId").value(rel.getId().toString()))
                .andExpect(jsonPath("$.type").value(RelationshipType.AGGREGATION.name()))
                .andExpect(jsonPath("$.sourceMultiplicity").value("0..1"))
                .andExpect(jsonPath("$.targetMultiplicity").value("*"));
    }
}
