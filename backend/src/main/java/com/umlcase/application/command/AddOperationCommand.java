package com.umlcase.application.command;

import com.umlcase.domain.model.Visibility;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AddOperationCommand {
    private final UUID commandId;
    private final UUID projectId;
    private final String participantId;
    private final long expectedVersion;
    private final UUID classId;
    private final String name;
    private final String returnType;
    private final Visibility visibility;
    private final List<ParameterData> parameters;

    @Data
    @Builder
    public static class ParameterData {
        private final String name;
        private final String type;
    }
}
