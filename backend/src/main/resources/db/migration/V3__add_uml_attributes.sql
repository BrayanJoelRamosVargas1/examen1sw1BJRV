CREATE TABLE uml_attributes (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    visibility VARCHAR(50) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_class_attributes FOREIGN KEY (class_id) REFERENCES uml_classes (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_class_attribute_name_lower ON uml_attributes (class_id, lower(name));
