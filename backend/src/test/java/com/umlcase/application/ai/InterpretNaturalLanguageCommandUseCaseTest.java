package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InterpretNaturalLanguageCommandUseCaseTest {

    private UmlModelRepository repository;
    private AiCommandInterpreter interpreter;
    private AiCommandResponseValidator validator;
    private InterpretNaturalLanguageCommandUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(UmlModelRepository.class);
        interpreter = Mockito.mock(AiCommandInterpreter.class);
        validator = new AiCommandResponseValidator();
        useCase = new InterpretNaturalLanguageCommandUseCase(repository, interpreter, validator);
    }

    @Test
    void testInterpretValidCommands() {
        UUID projectId = UUID.randomUUID();
        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(new UmlModel(projectId, UUID.randomUUID(), 1L)));

        InterpretedUmlCommand cmd1 = new InterpretedUmlCommand("CREATE_CLASS", "Cliente", null, null, null, null, null, null, null, null, null);
        when(interpreter.interpret(any(), any())).thenReturn(List.of(cmd1));

        List<InterpretedUmlCommand> result = useCase.execute(projectId, "crea clase Cliente");
        assertEquals(1, result.size());
        assertEquals("CREATE_CLASS", result.get(0).type());
    }

    @Test
    void testInterpretFiltersInvalidCommands() {
        UUID projectId = UUID.randomUUID();
        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(new UmlModel(projectId, UUID.randomUUID(), 1L)));

        InterpretedUmlCommand cmd1 = new InterpretedUmlCommand("CREATE_CLASS", "Cliente", null, null, null, null, null, null, null, null, null);
        InterpretedUmlCommand cmd2 = new InterpretedUmlCommand("DELETE_ALL", "Cliente", null, null, null, null, null, null, null, null, null);
        
        when(interpreter.interpret(any(), any())).thenReturn(List.of(cmd1, cmd2));

        List<InterpretedUmlCommand> result = useCase.execute(projectId, "crea clase y borra todo");
        assertEquals(1, result.size());
        assertEquals("CREATE_CLASS", result.get(0).type());
    }

    @Test
    void testTooManyCommands() {
        UUID projectId = UUID.randomUUID();
        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(new UmlModel(projectId, UUID.randomUUID(), 1L)));

        List<InterpretedUmlCommand> manyCommands = new java.util.ArrayList<>();
        for (int i = 0; i < 11; i++) {
            manyCommands.add(new InterpretedUmlCommand("CREATE_CLASS", "C" + i, null, null, null, null, null, null, null, null, null));
        }

        when(interpreter.interpret(any(), any())).thenReturn(manyCommands);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(projectId, "crea muchas"));
    }

    @Test
    void testFiltersReferenceToUnknownClass() {
        UUID projectId = UUID.randomUUID();
        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(new UmlModel(projectId, UUID.randomUUID(), 1L)));
        InterpretedUmlCommand command = new InterpretedUmlCommand(
            "ADD_ATTRIBUTE", "Cliente", null, "nombre", "String", null, null, null, null, null, null);
        when(interpreter.interpret(any(), any())).thenReturn(List.of(command));

        assertTrue(useCase.execute(projectId, "agrega nombre").isEmpty());
    }
}
