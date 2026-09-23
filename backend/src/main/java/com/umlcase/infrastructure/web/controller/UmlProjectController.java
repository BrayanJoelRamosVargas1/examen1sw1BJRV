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
    private final com.umlcase.application.handler.UpdateAttributeHandler updateAttributeHandler;
    private final com.umlcase.application.handler.RemoveAttributeHandler removeAttributeHandler;
    private final com.umlcase.application.handler.AddOperationHandler addOperationHandler;
    private final com.umlcase.application.handler.UpdateOperationHandler updateOperationHandler;
    private final com.umlcase.application.handler.RemoveOperationHandler removeOperationHandler;
    private final com.umlcase.application.port.in.AddRelationshipUseCase addRelationshipUseCase;
    private final com.umlcase.application.port.in.UpdateRelationshipUseCase updateRelationshipUseCase;
    private final com.umlcase.application.port.in.RemoveRelationshipUseCase removeRelationshipUseCase;
    private final com.umlcase.application.port.in.ExportModelUseCase exportModelUseCase;
    private final com.umlcase.application.port.in.ImportModelUseCase importModelUseCase;
    private final com.umlcase.application.port.in.GenerateRelationalSchemaUseCase generateRelationalSchemaUseCase;
    private final com.umlcase.application.port.out.RelationalSchemaExporter relationalSchemaExporter;

    public UmlProjectController(UmlModelRepository repository,
                                com.umlcase.application.handler.RenameClassHandler renameClassHandler,
                                com.umlcase.application.handler.AddAttributeHandler addAttributeHandler,
                                com.umlcase.application.handler.UpdateAttributeHandler updateAttributeHandler,
                                com.umlcase.application.handler.RemoveAttributeHandler removeAttributeHandler,
                                com.umlcase.application.handler.AddOperationHandler addOperationHandler,
                                com.umlcase.application.handler.UpdateOperationHandler updateOperationHandler,
                                com.umlcase.application.handler.RemoveOperationHandler removeOperationHandler,
                                com.umlcase.application.port.in.AddRelationshipUseCase addRelationshipUseCase,
                                com.umlcase.application.port.in.UpdateRelationshipUseCase updateRelationshipUseCase,
                                com.umlcase.application.port.in.RemoveRelationshipUseCase removeRelationshipUseCase,
                                com.umlcase.application.port.in.ExportModelUseCase exportModelUseCase,
                                com.umlcase.application.port.in.ImportModelUseCase importModelUseCase,
                                com.umlcase.application.port.in.GenerateRelationalSchemaUseCase generateRelationalSchemaUseCase,
                                com.umlcase.application.port.out.RelationalSchemaExporter relationalSchemaExporter) {
        this.repository = repository;
        this.renameClassHandler = renameClassHandler;
        this.addAttributeHandler = addAttributeHandler;
        this.updateAttributeHandler = updateAttributeHandler;
        this.removeAttributeHandler = removeAttributeHandler;
        this.addOperationHandler = addOperationHandler;
        this.updateOperationHandler = updateOperationHandler;
        this.removeOperationHandler = removeOperationHandler;
        this.addRelationshipUseCase = addRelationshipUseCase;
        this.updateRelationshipUseCase = updateRelationshipUseCase;
        this.removeRelationshipUseCase = removeRelationshipUseCase;
        this.exportModelUseCase = exportModelUseCase;
        this.importModelUseCase = importModelUseCase;
        this.generateRelationalSchemaUseCase = generateRelationalSchemaUseCase;
        this.relationalSchemaExporter = relationalSchemaExporter;
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
                    
                    List<com.umlcase.infrastructure.web.dto.UmlOperationDto> ops = c.getOperations().stream()
                            .map(op -> {
                                List<com.umlcase.infrastructure.web.dto.UmlParameterDto> params = op.getParameters().stream()
                                        .map(p -> new com.umlcase.infrastructure.web.dto.UmlParameterDto(
                                                p.getId(), p.getName(), p.getType(), op.getParameters().indexOf(p)
                                        )).collect(Collectors.toList());
                                return new com.umlcase.infrastructure.web.dto.UmlOperationDto(
                                        op.getId(), op.getName(), op.getReturnType(), op.getVisibility().name(), op.getOrderIndex(), params
                                );
                            }).collect(Collectors.toList());
                            
                    return new UmlClassDto(c.getId(), c.getName(), attrs, ops);
                })
                .collect(Collectors.toList());

        List<com.umlcase.infrastructure.web.dto.UmlRelationshipDto> relationships = model.getRelationships().stream()
                .map(r -> new com.umlcase.infrastructure.web.dto.UmlRelationshipDto(
                        r.getId(),
                        r.getType().name(),
                        r.getSourceClassId(),
                        r.getTargetClassId(),
                        r.getSourceMultiplicity(),
                        r.getTargetMultiplicity()
                ))
                .collect(Collectors.toList());

        UmlModelDto dto = new UmlModelDto(model.getProjectId(), model.getVersion(), classes, relationships);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(dto);
    }

    @GetMapping("/export/xmi")
    public ResponseEntity<byte[]> exportXmi(@PathVariable UUID projectId) {
        byte[] xmiData = exportModelUseCase.exportModel(projectId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/xml")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"uml-model.xmi\"")
                .body(xmiData);
    }

    @org.springframework.web.bind.annotation.PostMapping(value = "/import/xmi", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.umlcase.infrastructure.web.dto.ImportModelResponse> importXmi(
            @PathVariable UUID projectId,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam("commandId") String commandId,
            @org.springframework.web.bind.annotation.RequestParam("participantId") String participantId,
            @org.springframework.web.bind.annotation.RequestParam("expectedVersion") int expectedVersion) {

        try {
            com.umlcase.application.port.in.ImportModelCommand command = new com.umlcase.application.port.in.ImportModelCommand(
                    projectId,
                    participantId,
                    commandId,
                    expectedVersion,
                    file.getBytes()
            );

            UmlModel savedModel = importModelUseCase.importModel(command);

            var response = new com.umlcase.infrastructure.web.dto.ImportModelResponse(
                    commandId,
                    (int) savedModel.getVersion(),
                    savedModel.getClasses().size(),
                    savedModel.getRelationships().size()
            );

            return ResponseEntity.ok(response);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Error al leer el archivo XMI", e);
        }
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

    @org.springframework.web.bind.annotation.PutMapping("/classes/{classId}/attributes/{attributeId}")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.UpdateAttributeResponse> updateAttribute(
            @PathVariable UUID projectId,
            @PathVariable UUID classId,
            @PathVariable UUID attributeId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.UpdateAttributeRequest request) {

        var command = com.umlcase.application.command.UpdateAttributeCommand.builder()
                .commandId(request.commandId() != null ? request.commandId() : UUID.randomUUID())
                .projectId(projectId)
                .participantId(request.participantId())
                .expectedVersion(request.expectedVersion())
                .classId(classId)
                .attributeId(attributeId)
                .name(request.name())
                .type(request.type())
                .visibility(request.visibility())
                .build();

        var response = updateAttributeHandler.handle(command);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/classes/{classId}/attributes/{attributeId}")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.RemoveAttributeResponse> removeAttribute(
            @PathVariable UUID projectId,
            @PathVariable UUID classId,
            @PathVariable UUID attributeId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.RemoveAttributeRequest request) {
        var command = new com.umlcase.application.command.UmlCommand.RemoveAttribute(
                request.commandId() != null ? request.commandId() : UUID.randomUUID(),
                projectId,
                request.participantId(),
                request.expectedVersion(),
                classId,
                attributeId
        );
        return ResponseEntity.ok(removeAttributeHandler.handle(command));
    }

    @org.springframework.web.bind.annotation.PostMapping("/classes/{classId}/operations")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.AddOperationResponse> addOperation(
            @PathVariable UUID projectId,
            @PathVariable UUID classId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.AddOperationRequest request) {
        
        var command = com.umlcase.application.command.AddOperationCommand.builder()
                .commandId(request.getCommandId() != null ? request.getCommandId() : UUID.randomUUID())
                .projectId(projectId)
                .participantId(request.getParticipantId())
                .expectedVersion(request.getExpectedVersion())
                .classId(classId)
                .name(request.getName())
                .returnType(request.getReturnType())
                .visibility(request.getVisibility())
                .parameters(request.getParameters() != null ? request.getParameters().stream().map(p -> 
                        com.umlcase.application.command.AddOperationCommand.ParameterData.builder()
                        .name(p.getName())
                        .type(p.getType())
                        .build()
                ).collect(Collectors.toList()) : null)
                .build();

        var result = addOperationHandler.handle(command);
        
        var response = com.umlcase.infrastructure.web.dto.AddOperationResponse.builder()
                .commandId(command.getCommandId())
                .classId(classId)
                .operationId(result.operation().getId())
                .name(result.operation().getName())
                .returnType(result.operation().getReturnType())
                .visibility(result.operation().getVisibility())
                .orderIndex(result.operation().getOrderIndex())
                .parameters(result.operation().getParameters().stream().map(p -> 
                        com.umlcase.infrastructure.web.dto.AddOperationResponse.ParameterResponseDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .type(p.getType())
                        .orderIndex(result.operation().getParameters().indexOf(p))
                        .build()
                ).collect(Collectors.toList()))
                .modelVersion(result.newModelVersion())
                .build();
                
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/classes/{classId}/operations/{operationId}")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.UpdateOperationResponse> updateOperation(
            @PathVariable UUID projectId,
            @PathVariable UUID classId,
            @PathVariable UUID operationId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.UpdateOperationRequest request) {

        var command = new com.umlcase.application.command.UpdateOperationCommand(
                request.commandId() != null ? request.commandId() : UUID.randomUUID(),
                projectId,
                request.participantId(),
                request.expectedVersion(),
                classId,
                operationId,
                request.name(),
                request.returnType(),
                com.umlcase.domain.model.Visibility.valueOf(request.visibility()),
                request.parameters() != null ? request.parameters().stream().map(p ->
                        new com.umlcase.application.command.UpdateOperationCommand.ParameterData(p.id(), p.name(), p.type())
                ).collect(Collectors.toList()) : java.util.Collections.emptyList()
        );

        var event = updateOperationHandler.handle(command);

        var response = new com.umlcase.infrastructure.web.dto.UpdateOperationResponse(
                event.commandId(),
                event.classId(),
                event.operationId(),
                event.name(),
                event.returnType(),
                event.visibility().name(),
                event.orderIndex(),
                event.parameters().stream().map(p ->
                        new com.umlcase.infrastructure.web.dto.UmlParameterDto(p.id(), p.name(), p.type(), p.orderIndex())
                ).collect(Collectors.toList()),
                event.modelVersion()
        );

        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/classes/{classId}/operations/{operationId}")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.RemoveOperationResponse> removeOperation(
            @PathVariable UUID projectId,
            @PathVariable UUID classId,
            @PathVariable UUID operationId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.RemoveOperationRequest request) {

        com.umlcase.application.command.RemoveOperationCommand command = new com.umlcase.application.command.RemoveOperationCommand(
                request.commandId(),
                request.participantId(),
                projectId,
                classId,
                operationId,
                request.expectedVersion()
        );

        com.umlcase.application.event.OperationRemovedEvent event = removeOperationHandler.handle(command);

        com.umlcase.infrastructure.web.dto.RemoveOperationResponse response = new com.umlcase.infrastructure.web.dto.RemoveOperationResponse(
                event.commandId(),
                event.classId(),
                event.operationId(),
                event.modelVersion()
        );

        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PostMapping("/relationships")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.AddRelationshipResponse> addRelationship(
            @PathVariable UUID projectId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.AddRelationshipRequest request) {

        var command = new com.umlcase.application.command.UmlCommand.AddRelationship(
                request.commandId() != null ? request.commandId() : UUID.randomUUID(),
                projectId,
                request.participantId(),
                request.expectedVersion(),
                com.umlcase.domain.model.RelationshipType.valueOf(request.type()),
                request.sourceClassId(),
                request.targetClassId(),
                request.sourceMultiplicity(),
                request.targetMultiplicity()
        );

        var event = addRelationshipUseCase.handle(command);

        var response = new com.umlcase.infrastructure.web.dto.AddRelationshipResponse(
                event.commandId(),
                event.relationshipId(),
                event.modelVersion(),
                new com.umlcase.infrastructure.web.dto.UmlRelationshipDto(
                        event.relationshipId(),
                        event.relationshipType().name(),
                        event.sourceClassId(),
                        event.targetClassId(),
                        event.sourceMultiplicity(),
                        event.targetMultiplicity()
                )
        );

        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/relationships/{relationshipId}")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.UpdateRelationshipResponse> updateRelationship(
            @PathVariable UUID projectId,
            @PathVariable UUID relationshipId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.UpdateRelationshipRequest request) {

        var command = new com.umlcase.application.command.UmlCommand.UpdateRelationship(
                request.commandId() != null ? request.commandId() : UUID.randomUUID(),
                projectId,
                request.participantId(),
                request.expectedVersion(),
                relationshipId,
                request.type(),
                request.sourceMultiplicity(),
                request.targetMultiplicity()
        );

        var event = updateRelationshipUseCase.handle(command);

        var response = new com.umlcase.infrastructure.web.dto.UpdateRelationshipResponse(
                event.relationshipId(),
                event.type(),
                event.sourceMultiplicity(),
                event.targetMultiplicity(),
                event.modelVersion()
        );

        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/relationships/{relationshipId}")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.RemoveRelationshipResponse> removeRelationship(
            @PathVariable UUID projectId,
            @PathVariable UUID relationshipId,
            @org.springframework.web.bind.annotation.RequestBody com.umlcase.infrastructure.web.dto.RemoveRelationshipRequest request) {

        var command = new com.umlcase.application.command.UmlCommand.RemoveRelationship(
                request.commandId() != null ? request.commandId() : UUID.randomUUID(),
                projectId,
                request.participantId(),
                request.expectedVersion(),
                relationshipId
        );

        var event = removeRelationshipUseCase.handle(command);

        var response = new com.umlcase.infrastructure.web.dto.RemoveRelationshipResponse(
                event.commandId(),
                event.relationshipId(),
                event.modelVersion()
        );

        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.GetMapping("/relational-schema")
    public ResponseEntity<com.umlcase.infrastructure.web.dto.relational.RelationalSchemaResponse> getRelationalSchema(
            @PathVariable java.util.UUID projectId) {
        com.umlcase.domain.relational.RelationalSchema schema = generateRelationalSchemaUseCase.generateSchema(projectId);
        return ResponseEntity.ok(mapToResponse(schema));
    }

    private com.umlcase.infrastructure.web.dto.relational.RelationalSchemaResponse mapToResponse(com.umlcase.domain.relational.RelationalSchema schema) {
        return new com.umlcase.infrastructure.web.dto.relational.RelationalSchemaResponse(
            schema.getTables().stream().map(t -> new com.umlcase.infrastructure.web.dto.relational.RelationalTableResponse(
                t.getName(),
                t.getPrimaryKey() == null ? null : new com.umlcase.infrastructure.web.dto.relational.RelationalPrimaryKeyResponse(t.getPrimaryKey().columns()),
                t.getColumns().stream().map(c -> new com.umlcase.infrastructure.web.dto.relational.RelationalColumnResponse(c.name(), c.type(), c.isNullable())).toList(),
                t.getForeignKeys().stream().map(fk -> new com.umlcase.infrastructure.web.dto.relational.RelationalForeignKeyResponse(fk.name(), fk.columns(), fk.targetTable(), fk.targetColumns(), fk.onDeleteCascade())).toList(),
                t.getUniqueConstraints().stream().map(uc -> new com.umlcase.infrastructure.web.dto.relational.RelationalUniqueConstraintResponse(uc.name(), uc.columns())).toList()
            )).toList()
        );
    }

    @org.springframework.web.bind.annotation.GetMapping(value = "/export/sql", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<byte[]> exportSql(@PathVariable java.util.UUID projectId) {
        com.umlcase.domain.relational.RelationalSchema schema = generateRelationalSchemaUseCase.generateSchema(projectId);
        byte[] sqlData = relationalSchemaExporter.exportToSql(schema);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"schema.sql\"")
                .body(sqlData);
    }
}
