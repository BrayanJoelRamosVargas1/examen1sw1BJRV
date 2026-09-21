package com.umlcase.infrastructure.web;

import com.umlcase.application.handler.CreateClassHandler;
import com.umlcase.application.handler.RenameClassHandler;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.controller.UmlClassController;
import com.umlcase.infrastructure.web.controller.UmlProjectController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Optional;
import com.umlcase.domain.model.UmlModel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UmlProjectController.class, UmlClassController.class})
@Import(WebConfig.class)
@ActiveProfiles("dev")
@DisplayName("CorsWebConfigTest — Pruebas de configuración CORS en perfil dev")
class CorsWebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UmlModelRepository repository;

    @MockBean
    private CreateClassHandler createClassHandler;

    @MockBean
    private RenameClassHandler renameClassHandler;

    @MockBean
    private com.umlcase.application.handler.AddAttributeHandler addAttributeHandler;

    @MockBean
    private com.umlcase.application.handler.UpdateAttributeHandler updateAttributeHandler;

    @Test
    @DisplayName("GET /api/projects/{projectId}/model permite CORS desde localhost:4200")
    void cors_allowsGetFromLocalhost() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(repository.findByProjectId(any())).thenReturn(Optional.of(UmlModel.create(projectId)));

        mockMvc.perform(get("/api/projects/" + projectId + "/model")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    @DisplayName("OPTIONS /api/projects/{projectId}/classes autoriza el preflight POST desde localhost:4200")
    void cors_allowsPreflightPostFromLocalhost() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(options("/api/projects/" + projectId + "/classes")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PATCH,PUT,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "content-type"));
    }

    @Test
    @DisplayName("OPTIONS /api/projects/{projectId}/classes/{classId} autoriza el preflight PATCH desde localhost:4200")
    void cors_allowsPreflightPatchFromLocalhost() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        mockMvc.perform(options("/api/projects/" + projectId + "/classes/" + classId)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PATCH,PUT,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "content-type"));
    }

    @Test
    @DisplayName("OPTIONS /api/projects/{projectId}/classes/{classId}/attributes/{attributeId} autoriza el preflight PUT desde localhost:4200")
    void cors_allowsPreflightPutFromLocalhost() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();

        mockMvc.perform(options("/api/projects/" + projectId + "/classes/" + classId + "/attributes/" + attributeId)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PATCH,PUT,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "content-type"));
    }
}
