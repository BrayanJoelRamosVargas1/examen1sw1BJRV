package com.umlcase.infrastructure.web.dto.diagram;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetDiagramLayoutResponse {
    private String projectId;
    private long layoutVersion;
    private List<NodeViewDto> nodeViews;

    @Data
    @Builder
    public static class NodeViewDto {
        private String classId;
        private double x;
        private double y;
    }
}
