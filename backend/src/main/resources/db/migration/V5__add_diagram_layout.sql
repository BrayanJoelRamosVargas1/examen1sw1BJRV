CREATE TABLE uml_diagram_layouts (
    project_id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    last_modified TIMESTAMP NOT NULL
);

CREATE TABLE uml_node_views (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    class_id UUID NOT NULL,
    x FLOAT NOT NULL,
    y FLOAT NOT NULL,
    CONSTRAINT fk_node_views_layout FOREIGN KEY (project_id) REFERENCES uml_diagram_layouts(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_node_views_class FOREIGN KEY (class_id) REFERENCES uml_classes(id) ON DELETE CASCADE,
    CONSTRAINT uq_node_views_project_class UNIQUE (project_id, class_id)
);

-- Migrar proyectos existentes:
INSERT INTO uml_diagram_layouts (project_id, version, last_modified)
SELECT DISTINCT project_id, 0, CURRENT_TIMESTAMP
FROM uml_models;
