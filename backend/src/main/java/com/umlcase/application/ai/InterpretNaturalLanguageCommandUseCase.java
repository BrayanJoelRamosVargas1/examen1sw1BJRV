package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Locale;

@Service
public class InterpretNaturalLanguageCommandUseCase {

    private final UmlModelRepository modelRepository;
    private final AiCommandInterpreter interpreter;
    private final AiCommandResponseValidator validator;

    public InterpretNaturalLanguageCommandUseCase(UmlModelRepository modelRepository, AiCommandInterpreter interpreter, AiCommandResponseValidator validator) {
        this.modelRepository = modelRepository;
        this.interpreter = interpreter;
        this.validator = validator;
    }

    public List<InterpretedUmlCommand> execute(UUID projectId, String text) {
        if (text == null || text.isBlank() || text.trim().length() > 2000) {
            throw new IllegalArgumentException("Texto demasiado largo o vacío");
        }

        UmlModel model = modelRepository.findByProjectId(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        List<InterpretedUmlCommand> commands = interpreter.interpret(text.trim(), model);
        if (commands == null) {
            throw new IllegalStateException("El proveedor IA devolvió una respuesta vacía");
        }
        
        if (commands.size() > 10) {
            throw new IllegalArgumentException("Demasiados comandos devueltos por la IA (máximo 10)");
        }

        List<InterpretedUmlCommand> validCommands = new ArrayList<>();
        for (InterpretedUmlCommand cmd : commands) {
            if (validator.isValid(cmd) && referencesExistingClassesOrCreatesThem(cmd, model, validCommands)) {
                validCommands.add(cmd);
            }
        }
        
        return validCommands;
    }

    private boolean referencesExistingClassesOrCreatesThem(InterpretedUmlCommand command,
                                                            UmlModel model,
                                                            List<InterpretedUmlCommand> accepted) {
        return switch (command.type()) {
            case "CREATE_CLASS" -> true;
            case "RENAME_CLASS", "ADD_ATTRIBUTE", "ADD_OPERATION" -> classExistsOrCreated(
                command.className(), model, accepted);
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
