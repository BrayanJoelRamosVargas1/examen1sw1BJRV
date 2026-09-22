CREATE TABLE uml_relationships (
    id UUID PRIMARY KEY,
    model_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    source_class_id UUID NOT NULL,
    target_class_id UUID NOT NULL,
    source_multiplicity VARCHAR(50),
    target_multiplicity VARCHAR(50),
    CONSTRAINT fk_relationship_model FOREIGN KEY (model_id) REFERENCES uml_models (id) ON DELETE CASCADE,
    CONSTRAINT fk_relationship_source FOREIGN KEY (source_class_id) REFERENCES uml_classes (id) ON DELETE CASCADE,
    CONSTRAINT fk_relationship_target FOREIGN KEY (target_class_id) REFERENCES uml_classes (id) ON DELETE CASCADE
);
