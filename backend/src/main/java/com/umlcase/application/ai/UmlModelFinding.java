package com.umlcase.application.ai;

public record UmlModelFinding(
    String code,
    Severity severity,
    String message,
    String elementName
) {
    public enum Severity { INFO, WARNING, ERROR }
}
