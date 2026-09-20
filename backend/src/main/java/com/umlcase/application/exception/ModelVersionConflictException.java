package com.umlcase.application.exception;

public class ModelVersionConflictException extends RuntimeException {
    public ModelVersionConflictException(String message) {
        super(message);
    }
}
