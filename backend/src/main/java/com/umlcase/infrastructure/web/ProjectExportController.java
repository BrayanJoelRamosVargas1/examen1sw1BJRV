package com.umlcase.infrastructure.web;

import com.umlcase.application.port.in.GenerateBackendUseCase;
import com.umlcase.application.port.in.GeneratedProject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/generate")
public class ProjectExportController {

    private final GenerateBackendUseCase generateBackendUseCase;

    public ProjectExportController(GenerateBackendUseCase generateBackendUseCase) {
        this.generateBackendUseCase = generateBackendUseCase;
    }

    @GetMapping("/backend")
    public ResponseEntity<byte[]> generateBackend(@PathVariable UUID projectId) {
        GeneratedProject project = generateBackendUseCase.generate(projectId);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + project.filename() + "\"")
            .contentType(MediaType.parseMediaType("application/zip"))
            .body(project.content());
    }
}
