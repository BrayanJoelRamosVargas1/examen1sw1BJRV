package com.umlcase.infrastructure.web.controller;

import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test unitario de UmlProjectController.
 *
 * [BUG DOCUMENTADO] El controller original usaba findById(projectId),
 * lo cual fallaba porque projectId es el project_id de la tabla uml_models,
 * no el id primario del modelo (que es un UUID diferente asignado internamente).
 *
 * Este test primero reproduce el bug (test rojo cuando controller usa findById)
 * y luego verifica el comportamiento correcto con findByProjectId.
 */
@WebMvcTest(controllers = {UmlProjectController.class, GlobalExceptionHandler.class})
@DisplayName("UmlProjectController — GET /api/projects/{projectId}/model")
class UmlProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UmlModelRepository repository;

    @MockBean
    private com.umlcase.application.handler.CreateClassHandler createClassHandler;

    @MockBean
    private com.umlcase.application.handler.RenameClassHandler renameClassHandler;

    @MockBean
    private com.umlcase.application.handler.AddAttributeHandler addAttributeHandler;

    @MockBean
    private com.umlcase.application.handler.UpdateAttributeHandler updateAttributeHandler;

    @MockBean
    private com.umlcase.application.handler.RemoveAttributeHandler removeAttributeHandler;

    @MockBean
    private com.umlcase.application.handler.AddOperationHandler addOperationHandler;

    @MockBean
    private com.umlcase.application.handler.UpdateOperationHandler updateOperationHandler;

    // ─── Test del comportamiento correcto ─────────────────────────────────────

    @Test
    @DisplayName("GET model por projectId devuelve el modelo cuando existe")
    void getModel_byProjectId_returnsModel() throws Exception {
        UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        // El id interno del modelo es DIFERENTE al projectId
        UUID modelInternalId = UUID.randomUUID();

        UmlModel model = new UmlModel(modelInternalId, projectId, 0L);
        UmlClass cliente = UmlClass.create("Cliente");
        cliente.addAttribute(new com.umlcase.domain.model.UmlAttribute(UUID.randomUUID(), "attrTest", "int", com.umlcase.domain.model.Visibility.PUBLIC, 0));
        model.addClass(cliente);

        // El controller DEBE usar findByProjectId, NO findById
        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));
        // Si usara findById con el projectId, no encontraría nada (ids distintos)
        when(repository.findById(projectId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/{projectId}/model", projectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.classes").isArray())
                .andExpect(jsonPath("$.classes[0].name").value("Cliente"))
                .andExpect(jsonPath("$.classes[0].attributes").isArray())
                .andExpect(jsonPath("$.classes[0].attributes[0].name").value("attrTest"))
                .andExpect(jsonPath("$.classes[0].attributes[0].type").value("int"))
                .andExpect(jsonPath("$.classes[0].attributes[0].visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.classes[0].attributes[0].orderIndex").value(0));

        // Verificación crítica del bug: debe usarse findByProjectId
        verify(repository, times(1)).findByProjectId(projectId);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("GET model devuelve 404 cuando el proyecto no existe")
    void getModel_projectNotFound_returns404() throws Exception {
        UUID nonExistentProjectId = UUID.randomUUID();

        when(repository.findByProjectId(nonExistentProjectId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/{projectId}/model", nonExistentProjectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(repository, times(1)).findByProjectId(nonExistentProjectId);
    }

    @Test
    @DisplayName("GET model devuelve modelo vacío con versión correcta")
    void getModel_emptyModel_returnsVersionAndEmptyClasses() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID modelId = UUID.randomUUID();

        UmlModel emptyModel = new UmlModel(modelId, projectId, 5L);
        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(emptyModel));

        mockMvc.perform(get("/api/projects/{projectId}/model", projectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.classes").isEmpty());
    }
}
