package com.umlcase.infrastructure.web.dto;

import com.umlcase.domain.model.Visibility;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AddOperationResponse {
    private UUID commandId;
    private UUID classId;
    private UUID operationId;
    private String name;
    private String returnType;
    private Visibility visibility;
    private int orderIndex;
    private List<ParameterResponseDto> parameters;
    private long modelVersion;

    @Data
    @Builder
    public static class ParameterResponseDto {
        private UUID id;
        private String name;
        private String type;
        private int orderIndex;
    }
}
