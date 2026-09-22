package com.umlcase.infrastructure.persistence.entity.diagram;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "uml_diagram_layouts")
@Getter
@Setter
public class JpaUmlDiagramLayoutEntity {

    @Id
    @Column(name = "project_id")
    private java.util.UUID projectId;

    @Version
    private long version;

    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;

    @OneToMany(mappedBy = "diagramLayout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JpaUmlNodeViewEntity> nodeViews = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        markModified();
    }

    public void markModified() {
        Instant current = Instant.now();
        if (this.lastModified != null && !current.isAfter(this.lastModified)) {
            this.lastModified = this.lastModified.plusMillis(1);
        } else {
            this.lastModified = current;
        }
    }
}
