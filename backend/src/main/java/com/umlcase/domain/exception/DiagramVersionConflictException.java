package com.umlcase.domain.exception;

public class DiagramVersionConflictException extends RuntimeException {
    public DiagramVersionConflictException(String message) {
        super(message);
    }
}
