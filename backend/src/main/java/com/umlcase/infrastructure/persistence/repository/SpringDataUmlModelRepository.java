package com.umlcase.infrastructure.persistence.repository;

import com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SpringDataUmlModelRepository extends JpaRepository<JpaUmlModelEntity, UUID> {
    @EntityGraph(attributePaths = "classes")
    Optional<JpaUmlModelEntity> findByProjectId(UUID projectId);

    @EntityGraph(attributePaths = "classes")
    Optional<JpaUmlModelEntity> findById(UUID id);

    @Query("SELECT m FROM JpaUmlModelEntity m WHERE m.projectId = :projectId")
    Optional<JpaUmlModelEntity> lockForMutation(@Param("projectId") UUID projectId);
}
