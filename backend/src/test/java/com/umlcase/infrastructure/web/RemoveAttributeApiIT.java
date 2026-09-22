package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RemoveAttributeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RemoveAttributeApiIT {
    @Autowired MockMvc mockMvc;
    @Autowired UmlModelRepository repository;
    @Autowired ObjectMapper objectMapper;

    @Test
    void deleteReturns200JsonWithPersistedVersion() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("ApiRemove");
        model.addClass(umlClass);
        var attribute = model.addAttribute(umlClass.getId(), "a", "String", Visibility.PUBLIC);
        long version = repository.save(model).getVersion();
        var request = new RemoveAttributeRequest(UUID.randomUUID(), "p1", version);

        mockMvc.perform(delete("/api/projects/" + projectId + "/classes/" + umlClass.getId()
                + "/attributes/" + attribute.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandId").value(request.commandId().toString()))
                .andExpect(jsonPath("$.classId").value(umlClass.getId().toString()))
                .andExpect(jsonPath("$.attributeId").value(attribute.getId().toString()))
                .andExpect(jsonPath("$.modelVersion").value(version + 1));
    }

    @Test
    void staleVersionReturns409() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("ApiRemoveConflict");
        model.addClass(umlClass);
        var attribute = model.addAttribute(umlClass.getId(), "a", "String", Visibility.PUBLIC);
        long version = repository.save(model).getVersion();
        var request = new RemoveAttributeRequest(UUID.randomUUID(), "p1", version - 1);

        mockMvc.perform(delete("/api/projects/" + projectId + "/classes/" + umlClass.getId()
                + "/attributes/" + attribute.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MODEL_VERSION_CONFLICT"));
    }
}
