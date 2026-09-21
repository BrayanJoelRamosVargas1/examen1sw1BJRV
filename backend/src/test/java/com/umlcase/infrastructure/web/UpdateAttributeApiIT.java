package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.UpdateAttributeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UpdateAttributeApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UmlModelRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void put_updateAttribute_returns200() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazz = UmlClass.create("User");
        model.addClass(clazz);
        UmlAttribute attr = model.addAttribute(clazz.getId(), "oldName", "String", Visibility.PRIVATE);
        UmlModel savedModel = repository.save(model);

        UpdateAttributeRequest req = new UpdateAttributeRequest(
                UUID.randomUUID(), "p1", savedModel.getVersion(), "newName", "Integer", Visibility.PUBLIC
        );

        mockMvc.perform(put("/api/projects/" + projectId + "/classes/" + clazz.getId() + "/attributes/" + attr.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.commandId").value(req.commandId().toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.classId").value(clazz.getId().toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.attribute.id").value(attr.getId().toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.attribute.name").value("newName"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.attribute.type").value("Integer"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.attribute.visibility").value("PUBLIC"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.attribute.orderIndex").value(0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.modelVersion").value(savedModel.getVersion() + 1));
    }

    @Test
    void put_updateAttribute_returns409_whenVersionIsStale() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazz = UmlClass.create("User");
        model.addClass(clazz);
        UmlAttribute attr = model.addAttribute(clazz.getId(), "oldName", "String", Visibility.PRIVATE);
        UmlModel savedModel = repository.save(model);

        long staleVersion = savedModel.getVersion() - 1;

        UpdateAttributeRequest req = new UpdateAttributeRequest(
                UUID.randomUUID(), "p1", staleVersion, "newName", "Integer", Visibility.PUBLIC
        );

        mockMvc.perform(put("/api/projects/" + projectId + "/classes/" + clazz.getId() + "/attributes/" + attr.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("MODEL_VERSION_CONFLICT"));
    }
}
