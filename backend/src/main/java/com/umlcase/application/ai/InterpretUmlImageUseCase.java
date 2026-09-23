package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.ai.UmlImageFileValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InterpretUmlImageUseCase {
    private final UmlModelRepository modelRepository;
    private final UmlImageInterpreter interpreter;
    private final AiCommandResponseValidator commandValidator;
    private final UmlImageFileValidator fileValidator;

    public InterpretUmlImageUseCase(UmlModelRepository modelRepository,
                                    UmlImageInterpreter interpreter,
                                    AiCommandResponseValidator commandValidator,
                                    UmlImageFileValidator fileValidator) {
        this.modelRepository = modelRepository;
        this.interpreter = interpreter;
        this.commandValidator = commandValidator;
        this.fileValidator = fileValidator;
    }

    public UmlImageInterpretation execute(UUID projectId, byte[] image, String mimeType) {
        fileValidator.validate(image, mimeType);
        UmlModel model = modelRepository.findByProjectId(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        UmlImageInterpretation interpretation = interpreter.interpret(image, mimeType, model);
        if (interpretation == null) {
            throw new IllegalStateException("El proveedor visual devolvió una respuesta vacía");
        }
        if (interpretation.commands().size() > 10) {
            throw new IllegalArgumentException("Demasiados comandos devueltos por la IA (máximo 10)");
        }

        List<InterpretedUmlCommand> validCommands = new ArrayList<>();
        for (InterpretedUmlCommand command : interpretation.commands()) {
            if (commandValidator.isValid(command) && referencesKnownClasses(command, model, validCommands)) {
                validCommands.add(command);
            }
        }
        return new UmlImageInterpretation(validCommands, interpretation.warnings());
    }

    private boolean referencesKnownClasses(InterpretedUmlCommand command,
                                           UmlModel model,
                                           List<InterpretedUmlCommand> accepted) {
        return switch (command.type()) {
            case "CREATE_CLASS" -> true;
            case "RENAME_CLASS", "ADD_ATTRIBUTE", "ADD_OPERATION" ->
                classExistsOrCreated(command.className(), model, accepted);
            case "ADD_RELATIONSHIP" -> classExistsOrCreated(command.sourceClass(), model, accepted)
                && classExistsOrCreated(command.targetClass(), model, accepted);
            default -> false;
        };
    }

    private boolean classExistsOrCreated(String name, UmlModel model, List<InterpretedUmlCommand> accepted) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return model.getClasses().stream()
            .anyMatch(umlClass -> umlClass.getName().toLowerCase(Locale.ROOT).equals(normalized))
            || accepted.stream().anyMatch(command -> "CREATE_CLASS".equals(command.type())
                && command.className().toLowerCase(Locale.ROOT).equals(normalized));
    }
}
