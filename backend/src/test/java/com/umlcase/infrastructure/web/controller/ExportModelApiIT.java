package com.umlcase.infrastructure.web.controller;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ExportModelApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UmlModelRepository repository;

    @Test
    void testExportXmiEndpoint() throws Exception {
        // Setup empty project
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 1L);
        repository.save(model);

        Long initialVersion = model.getVersion();

        mockMvc.perform(get("/api/projects/{projectId}/export/xmi", projectId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/xml"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"uml-model.xmi\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("xmi:XMI")));

        // Verify version did not change (No mutation)
        UmlModel modelAfter = repository.findByProjectId(projectId).orElseThrow();
        assertEquals(initialVersion, modelAfter.getVersion());
    }
}
