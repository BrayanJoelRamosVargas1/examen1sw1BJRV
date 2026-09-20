CREATE TABLE uml_models (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL
);

CREATE TABLE uml_classes (
    id UUID PRIMARY KEY,
    model_id UUID NOT NULL REFERENCES uml_models(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL
);
