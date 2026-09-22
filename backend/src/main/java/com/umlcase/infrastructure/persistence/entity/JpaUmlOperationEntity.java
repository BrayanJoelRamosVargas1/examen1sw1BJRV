package com.umlcase.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "uml_operations")
@Getter
@Setter
@NoArgsConstructor
public class JpaUmlOperationEntity {

    @Id
    private UUID id;

    private String name;

    private String returnType;

    private String visibility;

    private int orderIndex;

    @jakarta.persistence.OneToMany(mappedBy = "operation", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("orderIndex ASC")
    private java.util.Set<JpaUmlParameterEntity> parameters = new java.util.LinkedHashSet<>();

    public void addParameter(JpaUmlParameterEntity param) {
        parameters.add(param);
        param.setOperation(this);
    }

    public void removeParameter(JpaUmlParameterEntity param) {
        parameters.remove(param);
        param.setOperation(null);
    }
}
