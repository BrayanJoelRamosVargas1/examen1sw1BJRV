CREATE TABLE processed_command (
    project_id VARCHAR(50) NOT NULL,
    command_id VARCHAR(50) NOT NULL,
    command_type VARCHAR(50) NOT NULL,
    request_fingerprint VARCHAR(255) NOT NULL,
    response_payload JSONB,
    model_version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_processed_command PRIMARY KEY (project_id, command_id)
);

CREATE INDEX idx_processed_command_project_id ON processed_command(project_id);
