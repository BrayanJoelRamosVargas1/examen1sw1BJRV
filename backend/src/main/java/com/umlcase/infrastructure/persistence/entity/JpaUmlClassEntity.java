package com.umlcase.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "uml_classes")
@Getter
@Setter
@NoArgsConstructor
public class JpaUmlClassEntity {

    @Id
    private UUID id;

    private String name;

    @jakarta.persistence.OneToMany(cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.JoinColumn(name = "class_id", nullable = false)
    @jakarta.persistence.OrderBy("orderIndex ASC")
    private java.util.List<JpaUmlAttributeEntity> attributes = new java.util.ArrayList<>();
}
