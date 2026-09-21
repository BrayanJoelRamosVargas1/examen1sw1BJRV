package com.umlcase.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "uml_attributes")
@Getter
@Setter
@NoArgsConstructor
public class JpaUmlAttributeEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String visibility;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;
}
