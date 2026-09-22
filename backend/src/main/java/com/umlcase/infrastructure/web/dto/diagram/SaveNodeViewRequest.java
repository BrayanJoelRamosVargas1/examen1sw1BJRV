package com.umlcase.infrastructure.web.dto.diagram;

import lombok.Data;

@Data
public class SaveNodeViewRequest {
    private String commandId;
    private String participantId;
    private long expectedLayoutVersion;
    private double x;
    private double y;
}
