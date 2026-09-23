package com.umlcase.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_command")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ProcessedCommandId.class)
public class ProcessedCommand {

    @Id
    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Id
    @Column(name = "command_id", nullable = false)
    private String commandId;

    @Column(name = "command_type", nullable = false)
    private String commandType;

    @Column(name = "request_fingerprint", nullable = false)
    private String requestFingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "model_version", nullable = false)
    private Long modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
