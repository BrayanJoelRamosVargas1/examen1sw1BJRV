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
                        JpaUmlClassEntity classEntity = existingChild.get();
                        classEntity.setName(domainClass.getName());
                        
                        // Merge attributes
                        if (classEntity.getAttributes() == null) {
                            classEntity.setAttributes(new java.util.ArrayList<>());
                        }
                        classEntity.getAttributes().removeIf(attr ->
                            domainClass.getAttributes().stream().noneMatch(ma -> ma.getId().equals(attr.getId()))
                        );
                        for (com.umlcase.domain.model.UmlAttribute domainAttr : domainClass.getAttributes()) {
                            Optional<com.umlcase.infrastructure.persistence.entity.JpaUmlAttributeEntity> existingAttr = classEntity.getAttributes().stream()
                                .filter(attr -> attr.getId().equals(domainAttr.getId()))
                                .findFirst();
                            if (existingAttr.isPresent()) {
                                existingAttr.get().setName(domainAttr.getName());
                                existingAttr.get().setType(domainAttr.getType());
                                existingAttr.get().setVisibility(domainAttr.getVisibility().name());
                                existingAttr.get().setOrderIndex(domainAttr.getOrderIndex());
                            } else {
                                com.umlcase.infrastructure.persistence.entity.JpaUmlAttributeEntity newAttr = new com.umlcase.infrastructure.persistence.entity.JpaUmlAttributeEntity();
                                newAttr.setId(domainAttr.getId());
                                newAttr.setName(domainAttr.getName());
                                newAttr.setType(domainAttr.getType());
                                newAttr.setVisibility(domainAttr.getVisibility().name());
                                newAttr.setOrderIndex(domainAttr.getOrderIndex());
                                classEntity.getAttributes().add(newAttr);
                            }
                        }
                    } else {
                        JpaUmlClassEntity newChild = new JpaUmlClassEntity();
                        newChild.setId(domainClass.getId());
                        newChild.setName(domainClass.getName());
                        newChild.setAttributes(domainClass.getAttributes().stream().map(a -> {
                            com.umlcase.infrastructure.persistence.entity.JpaUmlAttributeEntity attrEntity = new com.umlcase.infrastructure.persistence.entity.JpaUmlAttributeEntity();
                            attrEntity.setId(a.getId());
                            attrEntity.setName(a.getName());
                            attrEntity.setType(a.getType());
                            attrEntity.setVisibility(a.getVisibility().name());
                            attrEntity.setOrderIndex(a.getOrderIndex());
                            return attrEntity;
                        }).collect(java.util.stream.Collectors.toList()));
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
                UmlClass umlClass = new UmlClass(classEntity.getId(), classEntity.getName());
                if (classEntity.getAttributes() != null) {
                    classEntity.getAttributes().stream()
                            .sorted(java.util.Comparator.comparingInt(com.umlcase.infrastructure.persistence.entity.JpaUmlAttributeEntity::getOrderIndex))
                            .forEach(attrEntity -> umlClass.addAttribute(new com.umlcase.domain.model.UmlAttribute(
                                    attrEntity.getId(),
                                    attrEntity.getName(),
                                    attrEntity.getType(),
                                    com.umlcase.domain.model.Visibility.valueOf(attrEntity.getVisibility()),
                                    attrEntity.getOrderIndex()
                            )));
                }
                model.addClass(umlClass);
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
            classEntity.setAttributes(c.getAttributes().stream().map(a -> {
                com.umlcase.infrastructure.persistence.entity.JpaUmlAttributeEntity attrEntity = new com.umlcase.infrastructure.persistence.entity.JpaUmlAttributeEntity();
                attrEntity.setId(a.getId());
                attrEntity.setName(a.getName());
                attrEntity.setType(a.getType());
                attrEntity.setVisibility(a.getVisibility().name());
                attrEntity.setOrderIndex(a.getOrderIndex());
                return attrEntity;
            }).collect(java.util.stream.Collectors.toList()));
            return classEntity;
        }).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)));

        return entity;
    }
}
