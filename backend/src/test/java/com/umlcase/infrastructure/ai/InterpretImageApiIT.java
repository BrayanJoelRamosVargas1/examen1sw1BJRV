package com.umlcase.infrastructure.ai;

import com.umlcase.application.ai.InterpretedUmlCommand;
import com.umlcase.application.ai.UmlImageInterpretation;
import com.umlcase.application.ai.UmlImageInterpreter;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InterpretImageApiIT {
    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UmlModelRepository modelRepository;

    @MockBean
    private UmlImageInterpreter imageInterpreter;

    @BeforeEach
    void ensureProjectModelExists() {
        if (modelRepository.findByProjectId(PROJECT_ID).isEmpty()) {
            modelRepository.save(UmlModel.create(PROJECT_ID));
        }
    }

    @Test
    void interpretsMultipartImageWithoutMutatingModel() throws Exception {
        when(imageInterpreter.interpret(any(), eq("image/png"), any())).thenReturn(new UmlImageInterpretation(
            List.of(new InterpretedUmlCommand("CREATE_CLASS", "Cliente", null, null, null, null, null, null, null, null, null)),
            List.of("Una multiplicidad no fue legible")));

        String before = mockMvc.perform(get("/api/projects/{projectId}/model", PROJECT_ID))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long versionBefore = new com.fasterxml.jackson.databind.ObjectMapper().readTree(before).path("version").asLong();

        mockMvc.perform(multipart("/api/projects/{projectId}/ai/interpret-image", PROJECT_ID)
                .file(new MockMultipartFile("file", "diagram.png", "image/png", png())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.commands[0].type").value("CREATE_CLASS"))
            .andExpect(jsonPath("$.commands[0].className").value("Cliente"))
            .andExpect(jsonPath("$.warnings[0]").value("Una multiplicidad no fue legible"));

        mockMvc.perform(get("/api/projects/{projectId}/model", PROJECT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(versionBefore));
    }

    @Test
    void rejectsInvalidImageContent() throws Exception {
        mockMvc.perform(multipart("/api/projects/{projectId}/ai/interpret-image", PROJECT_ID)
                .file(new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes())))
            .andExpect(status().isBadRequest());
    }

    private byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }
}
