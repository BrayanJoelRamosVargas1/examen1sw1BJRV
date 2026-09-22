package com.umlcase.infrastructure.web.dto.diagram;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaveNodeViewResponse {
    private String commandId;
    private String classId;
    private double x;
    private double y;
    private long layoutVersion;
}
