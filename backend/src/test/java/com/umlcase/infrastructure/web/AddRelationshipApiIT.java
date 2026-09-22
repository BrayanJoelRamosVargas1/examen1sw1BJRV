package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.AddRelationshipRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AddRelationshipApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UmlModelRepository repository;

    @Test
    void post_addRelationship_returns201AndModelVersion() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazzA = UmlClass.create("ClassA");
        UmlClass clazzB = UmlClass.create("ClassB");
        model.addClass(clazzA);
        model.addClass(clazzB);
        UmlModel savedModel = repository.save(model);

        AddRelationshipRequest req = new AddRelationshipRequest(
                UUID.randomUUID(),
                "browser-1",
                savedModel.getVersion(),
                RelationshipType.ASSOCIATION.name(),
                clazzA.getId(),
                clazzB.getId(),
                "1",
                "*"
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/relationships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modelVersion").value(savedModel.getVersion() + 1))
                .andExpect(jsonPath("$.relationship.type").value(RelationshipType.ASSOCIATION.name()))
                .andExpect(jsonPath("$.relationship.sourceClassId").value(clazzA.getId().toString()))
                .andExpect(jsonPath("$.relationship.targetClassId").value(clazzB.getId().toString()));
    }
}
