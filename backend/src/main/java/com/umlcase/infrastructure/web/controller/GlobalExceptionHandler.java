package com.umlcase.infrastructure.web.controller;

import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.application.exception.ProjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<?> handleProjectNotFound(ProjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ModelVersionConflictException.class)
    public ResponseEntity<?> handleModelVersionConflict(ModelVersionConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new com.umlcase.infrastructure.web.dto.ApiErrorResponse("MODEL_VERSION_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(com.umlcase.domain.exception.DiagramVersionConflictException.class)
    public ResponseEntity<?> handleDiagramVersionConflict(com.umlcase.domain.exception.DiagramVersionConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new com.umlcase.infrastructure.web.dto.ApiErrorResponse("LAYOUT_VERSION_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }
}
