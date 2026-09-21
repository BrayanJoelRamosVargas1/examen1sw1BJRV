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
    public Optional<UmlModel> loadForUpdate(UUID projectId) {
        return springDataRepository.lockForMutation(projectId)
                .map(this::toDomain);
    }

    @Override
    public UmlModel save(UmlModel model) {
        try {
            JpaUmlModelEntity entity;
            Optional<JpaUmlModelEntity> existing = springDataRepository.findById(model.getId());

            if (existing.isPresent()) {
                entity = existing.get();
                if (!java.util.Objects.equals(model.getVersion(), entity.getVersion())) {
                    throw new ModelVersionConflictException(
                        String.format("Conflicto de concurrencia al guardar el modelo UML. Esperado: %d, Actual: %d",
                                model.getVersion(), entity.getVersion())
                    );
                }
                // Smart merge: update existing or add new
                // Remove deleted
                entity.getClasses().removeIf(c ->
                    model.getClasses().stream().noneMatch(mc -> mc.getId().equals(c.getId()))
                );
                // Update or Add
                for (UmlClass domainClass : model.getClasses()) {
                    Optional<JpaUmlClassEntity> existingChild = entity.getClasses().stream()
                        .filter(c -> c.getId().equals(domainClass.getId()))
                        .findFirst();
                    if (existingChild.isPresent()) {
                        existingChild.get().setName(domainClass.getName());
                    } else {
                        JpaUmlClassEntity newChild = new JpaUmlClassEntity();
                        newChild.setId(domainClass.getId());
                        newChild.setName(domainClass.getName());
                        entity.addClass(newChild);
                    }
                }
                // We rely on JPA's Optimistic Locking / OPTIMISTIC_FORCE_INCREMENT to manage version
            } else {
                entity = toEntity(model);
            }

            // Forzamos que la entidad se detecte como "sucia" (dirty check)
            // Esto provocará un UPDATE inmediato en el flush, incrementando automáticamente el @Version de la raíz
            if (entity.getId() != null) {
                entity.markModified();
            }

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
