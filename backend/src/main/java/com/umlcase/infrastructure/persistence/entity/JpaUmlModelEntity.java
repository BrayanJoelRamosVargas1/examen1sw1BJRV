package com.umlcase.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "uml_models")
@Getter
@Setter
@NoArgsConstructor
public class JpaUmlModelEntity {

    @Id
    private UUID id;

    private UUID projectId;

    @Version
    private Long version;

    @jakarta.persistence.Column(name = "last_modified", nullable = false)
    private Long lastModified = 0L;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "model_id", nullable = false)
    private java.util.Set<JpaUmlClassEntity> classes = new java.util.LinkedHashSet<>();

    public void addClass(JpaUmlClassEntity classEntity) {
        classes.add(classEntity);
    }

    public void markModified() {
        long current = System.currentTimeMillis();
        if (this.lastModified >= current) {
            this.lastModified++;
        } else {
            this.lastModified = current;
        }
    }
}
