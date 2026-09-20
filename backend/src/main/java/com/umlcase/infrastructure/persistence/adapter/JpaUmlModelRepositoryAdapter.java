package com.umlcase.infrastructure.persistence.adapter;

import com.umlcase.application.exception.ModelVersionConflictException;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.persistence.entity.JpaUmlClassEntity;
import com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity;
import com.umlcase.infrastructure.persistence.repository.SpringDataUmlModelRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JpaUmlModelRepositoryAdapter implements UmlModelRepository {

    private final SpringDataUmlModelRepository springDataRepository;

    public JpaUmlModelRepositoryAdapter(SpringDataUmlModelRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<UmlModel> findById(UUID modelId) {
        return springDataRepository.findById(modelId)
                .map(this::toDomain);
    }

    @Override
    public Optional<UmlModel> findByProjectId(UUID projectId) {
        return springDataRepository.findByProjectId(projectId)
                .map(this::toDomain);
    }

    @Override
    public UmlModel save(UmlModel model) {
        JpaUmlModelEntity entity = toEntity(model);
        try {
            JpaUmlModelEntity savedEntity = springDataRepository.saveAndFlush(entity);
            return toDomain(savedEntity);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ModelVersionConflictException("Conflicto de concurrencia al guardar el modelo UML. Otro usuario modificó el proyecto.");
        }
    }

    @Override
    public void deleteById(UUID modelId) {
        springDataRepository.deleteById(modelId);
    }

    @Override
    public boolean existsByProjectId(UUID projectId) {
        return springDataRepository.findByProjectId(projectId).isPresent();
    }

    private UmlModel toDomain(JpaUmlModelEntity entity) {
        UmlModel model = new UmlModel(entity.getId(), entity.getProjectId(), entity.getVersion());
        if (entity.getClasses() != null) {
            for (JpaUmlClassEntity classEntity : entity.getClasses()) {
                model.addClass(new UmlClass(classEntity.getId(), classEntity.getName()));
            }
        }
        return model;
    }

    private JpaUmlModelEntity toEntity(UmlModel model) {
        JpaUmlModelEntity entity = new JpaUmlModelEntity();
        entity.setId(model.getId());
        entity.setProjectId(model.getProjectId());
        entity.setVersion(model.getVersion());

        entity.setClasses(model.getClasses().stream().map(c -> {
            JpaUmlClassEntity classEntity = new JpaUmlClassEntity();
            classEntity.setId(c.getId());
            classEntity.setName(c.getName());
            return classEntity;
        }).collect(Collectors.toList()));

        return entity;
    }
}
