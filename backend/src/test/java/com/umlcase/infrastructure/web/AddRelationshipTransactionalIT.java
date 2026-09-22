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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AddRelationshipTransactionalIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UmlModelRepository repository;

    @Test
    void post_addRelationship_incrementsVersionExactlyOnce() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazzA = UmlClass.create("ClassA");
        UmlClass clazzB = UmlClass.create("ClassB");
        model.addClass(clazzA);
        model.addClass(clazzB);
        UmlModel savedModel = repository.save(model);
        
        long initialVersion = savedModel.getVersion();

        AddRelationshipRequest req = new AddRelationshipRequest(
                UUID.randomUUID(),
                "browser-1",
                initialVersion,
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
                .andExpect(jsonPath("$.modelVersion").value(initialVersion + 1));

        UmlModel dbAfterCommit = repository.findById(savedModel.getId()).orElseThrow();
        assertEquals(initialVersion + 1, dbAfterCommit.getVersion(), "dbAfterCommit.version == N+1");
        assertEquals(1, dbAfterCommit.getRelationships().size());
        assertEquals(RelationshipType.ASSOCIATION, dbAfterCommit.getRelationships().get(0).getType());
    }
}
