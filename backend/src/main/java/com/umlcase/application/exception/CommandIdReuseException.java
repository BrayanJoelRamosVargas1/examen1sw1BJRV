package com.umlcase.application.exception;

public class CommandIdReuseException extends RuntimeException {
    public CommandIdReuseException(String message) {
        super(message);
    }
}
