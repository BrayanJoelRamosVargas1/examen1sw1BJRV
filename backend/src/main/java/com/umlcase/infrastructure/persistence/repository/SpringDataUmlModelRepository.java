package com.umlcase.infrastructure.persistence.repository;

import com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUmlModelRepository extends JpaRepository<JpaUmlModelEntity, UUID> {
    @EntityGraph(attributePaths = "classes")
    Optional<JpaUmlModelEntity> findByProjectId(UUID projectId);
}
