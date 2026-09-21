package com.umlcase.infrastructure.web.controller;

import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.UmlClassDto;
import com.umlcase.infrastructure.web.dto.UmlModelDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class UmlProjectController {

    private final UmlModelRepository repository;
    private final com.umlcase.application.handler.RenameClassHandler renameClassHandler;
    private final com.umlcase.application.handler.AddAttributeHandler addAttributeHandler;


    public UmlProjectController(UmlModelRepository repository,
                                com.umlcase.application.handler.RenameClassHandler renameClassHandler,
                                com.umlcase.application.handler.AddAttributeHandler addAttributeHandler) {
        this.repository = repository;
        this.renameClassHandler = renameClassHandler;
        this.addAttributeHandler = addAttributeHandler;
    }

    @GetMapping("/model")
    public ResponseEntity<UmlModelDto> getModel(@PathVariable UUID projectId) {
        UmlModel model = repository.findByProjectId(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Proyecto no encontrado: " + projectId));

        List<UmlClassDto> classes = model.getClasses().stream()
                .map(c -> {
                    List<com.umlcase.infrastructure.web.dto.UmlAttributeDto> attrs = c.getAttributes().stream()
                            .map(a -> new com.umlcase.infrastructure.web.dto.UmlAttributeDto(
                                    a.getId(), a.getName(), a.getType(), a.getVisibility().name(), a.getOrderIndex()
                            )).collect(Collectors.toList());
                    return new UmlClassDto(c.getId(), c.getName(), attrs);
                })
                .collect(Collectors.toList());

        UmlModelDto dto = new UmlModelDto(model.getProjectId(), model.getVersion(), classes);
        return ResponseEntity.ok(dto);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/classes/{classId}")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.RenameClassResponse> renameClass(
            @PathVariable UUID projectId,
            @PathVariable UUID classId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.RenameClassRequest request) {

        var command = new com.umlcase.application.command.UmlCommand.RenameClass(
                UUID.fromString(request.commandId()),
                projectId,
                request.participantId(),
                request.expectedVersion(),
                classId,
                request.newName()
        );

        return ResponseEntity.ok(renameClassHandler.handle(command));
    }

    @org.springframework.web.bind.annotation.PostMapping("/classes/{classId}/attributes")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.AddAttributeResponse> addAttribute(
            @PathVariable UUID projectId,
            @PathVariable UUID classId,
            @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid com.umlcase.infrastructure.web.dto.AddAttributeRequest request) {

        var command = new com.umlcase.application.command.UmlCommand.AddAttribute(
                request.commandId() != null ? request.commandId() : UUID.randomUUID(),
                projectId,
                request.participantId(),
                request.expectedVersion(),
                classId,
                request.attributeName(),
                request.attributeType(),
                com.umlcase.domain.model.Visibility.valueOf(request.visibility())
        );

        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(addAttributeHandler.handle(command));
    }
}
