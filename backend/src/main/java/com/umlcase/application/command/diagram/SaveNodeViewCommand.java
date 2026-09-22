package com.umlcase.application.command.diagram;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaveNodeViewCommand {
    String commandId;
    String participantId;
    String projectId;
    String classId;
    long expectedLayoutVersion;
    double x;
    double y;
}
