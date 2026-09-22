package com.umlcase.infrastructure.web.dto;

import com.umlcase.domain.model.Visibility;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AddOperationRequest {
    private UUID commandId;
    private String participantId;
    private long expectedVersion;
    private String name;
    private String returnType;
    private Visibility visibility;
    private List<ParameterDto> parameters;

    @Data
    public static class ParameterDto {
        private String name;
        private String type;
    }
}
