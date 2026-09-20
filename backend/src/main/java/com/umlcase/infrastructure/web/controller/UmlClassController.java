package com.umlcase.infrastructure.web.controller;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.handler.CreateClassHandler;
import com.umlcase.infrastructure.web.dto.CreateClassRequest;
import com.umlcase.infrastructure.web.dto.CreateClassResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/classes")
public class UmlClassController {

    private final CreateClassHandler createClassHandler;

    public UmlClassController(CreateClassHandler createClassHandler) {
        this.createClassHandler = createClassHandler;
    }

    /**
     * POST /api/projects/{projectId}/classes
     *
     * Responde 201 Created con la versión realmente persistida.
     * El cliente A actualiza su currentVersion con response.modelVersion.
     * El cliente B la recibe mediante ClassCreatedEvent.modelVersion por WebSocket.
     */
    @PostMapping
    public ResponseEntity<CreateClassResponse> createClass(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateClassRequest request) {

        UmlCommand.CreateClass command = new UmlCommand.CreateClass(
                UUID.fromString(request.commandId()),
                projectId,
                request.participantId(),
                request.expectedVersion(),
                request.name()
        );

        CreateClassResponse response = createClassHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

