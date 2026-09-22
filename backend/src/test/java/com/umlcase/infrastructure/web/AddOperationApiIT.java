package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.AddOperationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AddOperationApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UmlModelRepository repository;

    @Test
    void post_addOperation_returns201AndModelVersion() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazz = UmlClass.create("Order");
        model.addClass(clazz);
        UmlModel savedModel = repository.save(model);

        AddOperationRequest req = new AddOperationRequest();
        req.setCommandId(UUID.randomUUID());
        req.setParticipantId("browser-1");
        req.setExpectedVersion(savedModel.getVersion());
        req.setName("calculateTotal");
        req.setReturnType("Double");
        req.setVisibility(Visibility.PUBLIC);
        
        AddOperationRequest.ParameterDto p1 = new AddOperationRequest.ParameterDto();
        p1.setName("tax");
        p1.setType("Double");
        req.setParameters(List.of(p1));

        mockMvc.perform(post("/api/projects/" + projectId + "/classes/" + clazz.getId() + "/operations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modelVersion").value(savedModel.getVersion() + 1))
                .andExpect(jsonPath("$.name").value("calculateTotal"))
                .andExpect(jsonPath("$.parameters[0].name").value("tax"));
    }
}
