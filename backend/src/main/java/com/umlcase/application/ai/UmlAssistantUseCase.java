package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UmlAssistantUseCase {
    private final UmlModelRepository modelRepository;
    private final UmlModelAnalyzer analyzer;
    private final UmlAssistant assistant;
    private final AiCommandResponseValidator commandValidator;
    private final UmlAssistantResponseValidator responseValidator;

    public UmlAssistantUseCase(UmlModelRepository modelRepository,
                               UmlModelAnalyzer analyzer,
                               UmlAssistant assistant,
                               AiCommandResponseValidator commandValidator,
                               UmlAssistantResponseValidator responseValidator) {
        this.modelRepository = modelRepository;
        this.analyzer = analyzer;
        this.assistant = assistant;
        this.commandValidator = commandValidator;
        this.responseValidator = responseValidator;
    }

    public UmlAssistantResponse execute(UUID projectId, String message) {
        if (message == null || message.isBlank() || message.length() > 2000) {
            throw new IllegalArgumentException("La pregunta debe tener entre 1 y 2000 caracteres");
        }
        UmlModel model = modelRepository.findByProjectId(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        List<UmlModelFinding> findings = analyzer.analyze(model);
        UmlAssistantResponse response = assistant.answer(message.trim(), model, findings);
        if (!responseValidator.isValid(response)) {
            throw new IllegalStateException("El asistente devolvió una respuesta vacía");
        }

        List<InterpretedUmlCommand> validCommands = new ArrayList<>();
        for (InterpretedUmlCommand command : response.suggestedCommands()) {
            if (commandValidator.isValid(command) && referencesExistingOrSuggestedClass(command, model, validCommands)) {
                validCommands.add(command);
            }
        }
        return new UmlAssistantResponse(response.answer(), findings, validCommands, response.warnings());
    }

    private boolean referencesExistingOrSuggestedClass(InterpretedUmlCommand command,
                                                        UmlModel model,
                                                        List<InterpretedUmlCommand> accepted) {
        return switch (command.type()) {
            case "CREATE_CLASS" -> true;
            case "RENAME_CLASS", "ADD_ATTRIBUTE", "ADD_OPERATION" -> classExistsOrCreated(command.className(), model, accepted);
            case "ADD_RELATIONSHIP" -> classExistsOrCreated(command.sourceClass(), model, accepted)
                && classExistsOrCreated(command.targetClass(), model, accepted);
            default -> false;
        };
    }

    private boolean classExistsOrCreated(String name, UmlModel model, List<InterpretedUmlCommand> accepted) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return model.getClasses().stream().anyMatch(umlClass -> umlClass.getName().toLowerCase(Locale.ROOT).equals(normalized))
            || accepted.stream().anyMatch(command -> "CREATE_CLASS".equals(command.type())
                && command.className().toLowerCase(Locale.ROOT).equals(normalized));
    }
}
