package com.umlcase.application.handler.diagram;

import com.umlcase.application.command.diagram.SaveNodeViewCommand;
import com.umlcase.application.exception.ProjectNotFoundException;
import com.umlcase.application.port.out.diagram.DiagramEventPublisher;
import com.umlcase.application.port.out.diagram.UmlDiagramLayoutRepository;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.diagram.UmlDiagramLayout;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SaveNodeViewHandlerTest {

    @Mock
    private UmlDiagramLayoutRepository diagramRepository;

    @Mock
    private UmlModelRepository modelRepository;

    @Mock
    private DiagramEventPublisher diagramEventPublisher;

    private SaveNodeViewHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new SaveNodeViewHandler(diagramRepository, modelRepository, diagramEventPublisher);
    }

    @Test
    void testSaveNodeViewSuccessfully() {
        String projectId = UUID.randomUUID().toString();
        String classId = UUID.randomUUID().toString();

        UmlModel model = new UmlModel(UUID.fromString(projectId), UUID.fromString(projectId), 1L);
        model.addClass(new UmlClass(UUID.fromString(classId), "ClassA"));

        UmlDiagramLayout layout = UmlDiagramLayout.builder()
                .projectId(projectId)
                .version(0)
                .nodeViews(new ArrayList<>())
                .build();

        when(modelRepository.findByProjectId(any())).thenReturn(Optional.of(model));
        when(diagramRepository.findByProjectId(projectId)).thenReturn(Optional.of(layout));
        when(diagramRepository.save(any(), anyLong())).thenReturn(1L);

        SaveNodeViewCommand command = SaveNodeViewCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .participantId("P1")
                .projectId(projectId)
                .classId(classId)
                .expectedLayoutVersion(0)
                .x(100.5)
                .y(200.5)
                .build();

        long newVersion = handler.execute(command);

        assertEquals(1, newVersion);

        ArgumentCaptor<UmlDiagramLayout> captor = ArgumentCaptor.forClass(UmlDiagramLayout.class);
        verify(diagramRepository).save(captor.capture(), eq(0L));
        UmlDiagramLayout saved = captor.getValue();
        assertEquals(1, saved.getNodeViews().size());
        assertEquals(classId, saved.getNodeViews().get(0).getClassId());
        assertEquals(100.5, saved.getNodeViews().get(0).getX());
        assertEquals(200.5, saved.getNodeViews().get(0).getY());

        ArgumentCaptor<com.umlcase.application.event.NodeMovedEvent> eventCaptor = ArgumentCaptor.forClass(com.umlcase.application.event.NodeMovedEvent.class);
        verify(diagramEventPublisher, times(1)).publish(eventCaptor.capture());

        com.umlcase.application.event.NodeMovedEvent publishedEvent = eventCaptor.getValue();
        assertEquals("NODE_MOVED", publishedEvent.getEventType());
        assertEquals(projectId, publishedEvent.getProjectId());
        assertEquals(classId, publishedEvent.getClassId());
        assertEquals(100.5, publishedEvent.getX());
        assertEquals(200.5, publishedEvent.getY());
        assertEquals(1L, publishedEvent.getLayoutVersion());
    }

    @Test
    void testSaveNodeViewInvalidCoordinates() {
        SaveNodeViewCommand command = SaveNodeViewCommand.builder()
                .x(Double.NaN)
                .y(100)
                .build();

        assertThrows(IllegalArgumentException.class, () -> handler.execute(command));
    }

    @Test
    void testSaveNodeViewProjectNotFound() {
        when(modelRepository.findByProjectId(any())).thenReturn(Optional.empty());

        SaveNodeViewCommand command = SaveNodeViewCommand.builder()
                .projectId(UUID.randomUUID().toString())
                .x(100)
                .y(100)
                .build();

        assertThrows(ProjectNotFoundException.class, () -> handler.execute(command));
    }

    @Test
    void testSaveNodeViewClassNotFound() {
        String projectId = UUID.randomUUID().toString();
        UmlModel model = new UmlModel(UUID.fromString(projectId), UUID.fromString(projectId), 1L);
        when(modelRepository.findByProjectId(any())).thenReturn(Optional.of(model));

        SaveNodeViewCommand command = SaveNodeViewCommand.builder()
                .projectId(projectId)
                .classId("missing-class")
                .x(100)
                .y(100)
                .build();

        assertThrows(IllegalArgumentException.class, () -> handler.execute(command));
    }
}
