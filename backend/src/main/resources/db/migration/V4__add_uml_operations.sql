CREATE TABLE uml_operations (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    return_type VARCHAR(255) NOT NULL,
    visibility VARCHAR(50) NOT NULL,
    order_index INT NOT NULL,
    CONSTRAINT fk_uml_operation_class FOREIGN KEY (class_id) REFERENCES uml_classes(id) ON DELETE CASCADE
);

CREATE TABLE uml_parameters (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    order_index INT NOT NULL,
    CONSTRAINT fk_uml_parameter_operation FOREIGN KEY (operation_id) REFERENCES uml_operations(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_unique_parameter_name ON uml_parameters (operation_id, LOWER(name));
