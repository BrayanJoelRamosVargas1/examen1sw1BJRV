package com.umlcase.infrastructure.persistence.entity.diagram;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "uml_node_views")
@Getter
@Setter
public class JpaUmlNodeViewEntity {

    @Id
    private java.util.UUID id;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "project_id", nullable = false)
    private JpaUmlDiagramLayoutEntity diagramLayout;
    
    private java.util.UUID classId;

    private double x;
    private double y;
}
