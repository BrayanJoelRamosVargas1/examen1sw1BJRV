package com.umlcase.application.event;

public class NodeMovedEvent {
    private final String eventType = "NODE_MOVED";
    private final String commandId;
    private final String projectId;
    private final String classId;
    private final double x;
    private final double y;
    private final long layoutVersion;

    public NodeMovedEvent(String commandId, String projectId, String classId, double x, double y, long layoutVersion) {
        this.commandId = commandId;
        this.projectId = projectId;
        this.classId = classId;
        this.x = x;
        this.y = y;
        this.layoutVersion = layoutVersion;
    }

    public String getEventType() {
        return eventType;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getClassId() {
        return classId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public long getLayoutVersion() {
        return layoutVersion;
    }
}
