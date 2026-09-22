package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.UmlParameterDto;
import com.umlcase.infrastructure.web.dto.UpdateOperationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Transactional
class UpdateOperationApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UmlModelRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID projectId;
    private UmlClass cls;
    private UmlOperation operation;
    private long savedModelVersion;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        cls = UmlClass.create("C1");
        operation = UmlOperation.create("op1", "void", Visibility.PUBLIC);
        cls.addOperation(operation);
        model.addClass(cls);
        savedModelVersion = repository.save(model).getVersion();
    }

    @Test
    void shouldUpdateOperationAndReturn200() throws Exception {
        var req = new UpdateOperationRequest(
                UUID.randomUUID(), "u1", savedModelVersion, "op2", "String", "PRIVATE",
                List.of(new UmlParameterDto(null, "p1", "Integer", 0))
        );

        mockMvc.perform(put("/api/projects/{projectId}/classes/{classId}/operations/{operationId}", projectId, cls.getId(), operation.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("op2"))
                .andExpect(jsonPath("$.returnType").value("String"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.parameters", hasSize(1)))
                .andExpect(jsonPath("$.parameters[0].name").value("p1"))
                .andExpect(jsonPath("$.modelVersion").value(savedModelVersion + 1));
    }

    @Test
    void shouldReturn409OnVersionConflict() throws Exception {
        var req = new UpdateOperationRequest(
                UUID.randomUUID(), "u1", 99, "op2", "String", "PRIVATE", List.of()
        );

        mockMvc.perform(put("/api/projects/{projectId}/classes/{classId}/operations/{operationId}", projectId, cls.getId(), operation.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}
