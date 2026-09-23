package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.ai.UmlImageFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterpretUmlImageUseCaseTest {
    private UmlModelRepository repository;
    private UmlImageInterpreter interpreter;
    private InterpretUmlImageUseCase useCase;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        repository = mock(UmlModelRepository.class);
        interpreter = mock(UmlImageInterpreter.class);
        useCase = new InterpretUmlImageUseCase(repository, interpreter,
            new AiCommandResponseValidator(), new UmlImageFileValidator());
        projectId = UUID.randomUUID();
        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(new UmlModel(projectId, UUID.randomUUID(), 7L)));
    }

    @Test
    void returnsValidatedCommandsAndWarningsWithoutSaving() {
        InterpretedUmlCommand command = new InterpretedUmlCommand(
            "CREATE_CLASS", "Cliente", null, null, null, null, null, null, null, null, null);
        when(interpreter.interpret(any(), any(), any())).thenReturn(new UmlImageInterpretation(
            List.of(command), List.of("Multiplicidad no legible")));

        UmlImageInterpretation result = useCase.execute(projectId, png(), "image/png");

        assertEquals(List.of(command), result.commands());
        assertEquals(List.of("Multiplicidad no legible"), result.warnings());
        verify(repository).findByProjectId(projectId);
    }

    @Test
    void filtersUnsupportedCommandsAndRejectsInvalidImages() {
        when(interpreter.interpret(any(), any(), any())).thenReturn(new UmlImageInterpretation(List.of(
            new InterpretedUmlCommand("DROP_DATABASE", "Cliente", null, null, null, null, null, null, null, null, null)
        ), List.of()));
        assertEquals(0, useCase.execute(projectId, png(), "image/png").commands().size());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(projectId, "bad".getBytes(), "image/png"));
    }

    private byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }
}
