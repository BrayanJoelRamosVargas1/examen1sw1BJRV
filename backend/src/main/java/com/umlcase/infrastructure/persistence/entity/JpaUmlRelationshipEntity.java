package com.umlcase.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "uml_relationships")
public class JpaUmlRelationshipEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private JpaUmlModelEntity model;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "source_class_id", nullable = false)
    private UUID sourceClassId;

    @Column(name = "target_class_id", nullable = false)
    private UUID targetClassId;

    @Column(name = "source_multiplicity", length = 50)
    private String sourceMultiplicity;

    @Column(name = "target_multiplicity", length = 50)
    private String targetMultiplicity;

    protected JpaUmlRelationshipEntity() {}

    public JpaUmlRelationshipEntity(UUID id, JpaUmlModelEntity model, String type,
                                    UUID sourceClassId, UUID targetClassId,
                                    String sourceMultiplicity, String targetMultiplicity) {
        this.id = id;
        this.model = model;
        this.type = type;
        this.sourceClassId = sourceClassId;
        this.targetClassId = targetClassId;
        this.sourceMultiplicity = sourceMultiplicity;
        this.targetMultiplicity = targetMultiplicity;
    }

    public UUID getId() { return id; }
    public JpaUmlModelEntity getModel() { return model; }
    public void setModel(JpaUmlModelEntity model) { this.model = model; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public UUID getSourceClassId() { return sourceClassId; }
    public void setSourceClassId(UUID sourceClassId) { this.sourceClassId = sourceClassId; }
    public UUID getTargetClassId() { return targetClassId; }
    public void setTargetClassId(UUID targetClassId) { this.targetClassId = targetClassId; }
    public String getSourceMultiplicity() { return sourceMultiplicity; }
    public void setSourceMultiplicity(String sourceMultiplicity) { this.sourceMultiplicity = sourceMultiplicity; }
    public String getTargetMultiplicity() { return targetMultiplicity; }
    public void setTargetMultiplicity(String targetMultiplicity) { this.targetMultiplicity = targetMultiplicity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaUmlRelationshipEntity other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
