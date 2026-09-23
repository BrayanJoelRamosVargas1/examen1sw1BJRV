package com.umlcase.infrastructure.persistence.repository;

import com.umlcase.infrastructure.persistence.entity.ProcessedCommand;
import com.umlcase.infrastructure.persistence.entity.ProcessedCommandId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, ProcessedCommandId> {
}
