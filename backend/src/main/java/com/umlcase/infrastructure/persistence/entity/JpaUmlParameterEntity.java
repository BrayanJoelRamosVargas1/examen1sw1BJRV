package com.umlcase.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "uml_parameters")
@Getter
@Setter
@NoArgsConstructor
public class JpaUmlParameterEntity {

    @Id
    private UUID id;

    private String name;
    
    private String type;
    
    private int orderIndex;
}
