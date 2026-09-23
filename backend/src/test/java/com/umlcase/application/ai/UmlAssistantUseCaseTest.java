package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UmlAssistantUseCaseTest {
    private UmlModelRepository repository;
    private UmlAssistant assistant;
    private UmlAssistantUseCase useCase;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        repository = mock(UmlModelRepository.class);
        assistant = mock(UmlAssistant.class);
        useCase = new UmlAssistantUseCase(repository, new UmlModelAnalyzer(), assistant,
            new AiCommandResponseValidator(), new UmlAssistantResponseValidator());
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        model.addClass(UmlClass.create("Producto"));
        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));
    }

    @Test
    void addsDeterministicFindingsAndKeepsSuggestionsReadOnly() {
        when(assistant.answer(any(), any(), any())).thenReturn(new UmlAssistantResponse(
            "Producto está aislado.", List.of(), List.of(), List.of()));

        UmlAssistantResponse result = useCase.execute(projectId, "¿Qué le falta a mi modelo?");

        assertEquals("Producto está aislado.", result.answer());
        org.junit.jupiter.api.Assertions.assertTrue(result.findings().stream()
            .anyMatch(finding -> finding.code().equals("ISOLATED_CLASS")));
        assertEquals(0L, repository.findByProjectId(projectId).orElseThrow().getVersion());
    }

    @Test
    void filtersUnsupportedAndUnknownClassSuggestions() {
        when(assistant.answer(any(), any(), any())).thenReturn(new UmlAssistantResponse(
            "Sugerencias", List.of(), List.of(
                new InterpretedUmlCommand("ADD_ATTRIBUTE", "Fantasma", null, "x", "String", null, null, null, null, null, null),
                new InterpretedUmlCommand("DROP_DATABASE", "Producto", null, null, null, null, null, null, null, null, null)
            ), List.of()));

        assertEquals(0, useCase.execute(projectId, "sugiere mejoras").suggestedCommands().size());
    }

    @Test
    void rejectsOversizedQuestion() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(projectId, "x".repeat(2001)));
    }
}
