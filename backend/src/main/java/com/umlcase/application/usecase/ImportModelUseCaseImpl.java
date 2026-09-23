package com.umlcase.application.usecase;

import com.umlcase.application.port.in.ImportModelCommand;
import com.umlcase.application.port.in.ImportModelUseCase;
import com.umlcase.application.port.out.UmlInterchangeImporter;
import com.umlcase.application.event.ModelImportedEvent;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImportModelUseCaseImpl implements ImportModelUseCase {

    private final UmlModelRepository repository;
    private final UmlInterchangeImporter importer;
    private final ApplicationEventPublisher eventPublisher;

    public ImportModelUseCaseImpl(UmlModelRepository repository, UmlInterchangeImporter importer, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.importer = importer;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UmlModel importModel(ImportModelCommand command) {
        // 1. Obtener modelo actual (para validar version y reemplazar)
        UmlModel currentModel = repository.findByProjectId(command.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Model not found for project: " + command.projectId()));

        // Validar expected version
        if (currentModel.getVersion() != command.expectedVersion()) {
            throw new com.umlcase.application.exception.ModelVersionConflictException("Versión del modelo incorrecta. Esperada: " + command.expectedVersion() + ", Actual: " + currentModel.getVersion());
        }

        // 2. Parsear el XMI a un nuevo modelo semántico (transitorio)
        UmlModel importedModel = importer.importModel(command.xmiContent());

        // 3. Reemplazar contenido (mantiene ID, projectId, y versión que JPA incrementará automáticamente)
        currentModel.replaceWith(importedModel);

        // 4. Guardar (genera update con version + 1 y elimina viejas referencias, etc)
        UmlModel savedModel = repository.save(currentModel);

        // 5. Emitir evento AFTER_COMMIT
        eventPublisher.publishEvent(new ModelImportedEvent(
                savedModel.getProjectId(),
                command.participantId(),
                command.commandId(),
                (int) savedModel.getVersion(),
                savedModel.getClasses().size(),
                savedModel.getRelationships().size()
        ));

        return savedModel;
    }
}
