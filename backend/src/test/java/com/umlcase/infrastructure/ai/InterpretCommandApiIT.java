package com.umlcase.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.umlcase.application.ai.AiCommandInterpreter;
import com.umlcase.application.ai.InterpretedUmlCommand;
import com.umlcase.application.ai.UmlAssistant;
import com.umlcase.application.ai.UmlAssistantResponse;
import com.umlcase.application.ai.UmlModelFinding;
import com.umlcase.domain.port.UmlModelRepository;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InterpretCommandApiIT {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiCommandInterpreter aiCommandInterpreter;

    @MockBean
    private UmlAssistant umlAssistant;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private UmlModelRepository modelRepository;

    @BeforeEach
    void ensureProjectModelExists() {
        if (modelRepository.findByProjectId(PROJECT_ID).isEmpty()) {
            modelRepository.save(com.umlcase.domain.model.UmlModel.create(PROJECT_ID));
        }
    }

    @Test
    void shouldReturnInterpretedCommands() throws Exception {
        UUID projectId = PROJECT_ID;

        InterpretedUmlCommand cmd = new InterpretedUmlCommand("CREATE_CLASS", "Cliente", null, null, null, null, null, null, null, null, null);
        when(aiCommandInterpreter.interpret(eq("crear cliente"), any())).thenReturn(List.of(cmd));

        String versionBefore = mockMvc.perform(get("/api/projects/{projectId}/model", projectId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/projects/{projectId}/ai/interpret", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"crear cliente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CREATE_CLASS"))
                .andExpect(jsonPath("$[0].className").value("Cliente"));

        mockMvc.perform(get("/api/projects/{projectId}/model", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(
                    com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                        .readTree(versionBefore).path("version").asLong()));
    }

    @Test
    void rejectsOversizedText() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/ai/interpret", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"" + "x".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTooManyCommands() throws Exception {
        UUID projectId = PROJECT_ID;
        List<InterpretedUmlCommand> commands = java.util.stream.IntStream.range(0, 11)
            .mapToObj(index -> new InterpretedUmlCommand("CREATE_CLASS", "Clase" + index,
                null, null, null, null, null, null, null, null, null))
            .toList();
        when(aiCommandInterpreter.interpret(eq("muchas"), any())).thenReturn(commands);

        mockMvc.perform(post("/api/projects/{projectId}/ai/interpret", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"muchas\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsEmptyStructuredResultForUnsupportedProviderCommand() throws Exception {
        UUID projectId = PROJECT_ID;
        when(aiCommandInterpreter.interpret(eq("inseguro"), any())).thenReturn(List.of(
            new InterpretedUmlCommand("DELETE_DATABASE", "Cliente", null, null, null, null, null, null, null, null, null)
        ));

        mockMvc.perform(post("/api/projects/{projectId}/ai/interpret", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"inseguro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void mapsInvalidProviderOutputToBadGateway() throws Exception {
        UUID projectId = PROJECT_ID;
        when(aiCommandInterpreter.interpret(eq("respuesta invalida"), any())).thenReturn(null);

        mockMvc.perform(post("/api/projects/{projectId}/ai/interpret", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"respuesta invalida\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void startsWithAiDisabledAndNoApiKeyRequired() {
        org.junit.jupiter.api.Assertions.assertFalse(aiProperties.isEnabled());
        org.junit.jupiter.api.Assertions.assertTrue(aiProperties.getApiKey().isBlank());
    }

    @Test
    void assistantReturnsStructuredReadOnlyResponse() throws Exception {
        when(umlAssistant.answer(eq("¿Qué le falta?"), any(), any())).thenReturn(new UmlAssistantResponse(
            "Se analizaron las clases.",
            List.of(new UmlModelFinding("ISOLATED_CLASS", UmlModelFinding.Severity.INFO,
                "Cliente está aislada.", "Cliente")),
            List.of(new InterpretedUmlCommand("CREATE_CLASS", "Factura", null, null, null, null, null, null, null, null, null)),
            List.of()));

        String before = mockMvc.perform(get("/api/projects/{projectId}/model", PROJECT_ID))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long versionBefore = new com.fasterxml.jackson.databind.ObjectMapper().readTree(before).path("version").asLong();

        mockMvc.perform(post("/api/projects/{projectId}/ai/assistant", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"¿Qué le falta?\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").value("Se analizaron las clases."))
            .andExpect(jsonPath("$.findings[0].code").value("EMPTY_MODEL"))
            .andExpect(jsonPath("$.suggestedCommands[0].className").value("Factura"));

        mockMvc.perform(get("/api/projects/{projectId}/model", PROJECT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(versionBefore));
    }

    @Test
    void assistantRejectsOversizedMessage() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/ai/assistant", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"" + "x".repeat(2001) + "\"}"))
            .andExpect(status().isBadRequest());
    }
}
