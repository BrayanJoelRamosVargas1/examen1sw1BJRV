package com.umlcase.application.handler;

import com.umlcase.application.command.RemoveOperationCommand;
import com.umlcase.application.event.OperationRemovedEvent;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveOperationHandlerTest {

    @Mock
    private UmlModelRepository repository;

    @Mock
    private UmlEventPublisher publisher;

    @InjectMocks
    private RemoveOperationHandler handler;

    @Test
    void testHandleSuccess() {
        UUID projectId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 5L);
        UmlClass cls = new UmlClass(classId, "TestClass");
        UmlOperation op = new UmlOperation(operationId, "testOp", "void", com.umlcase.domain.model.Visibility.PUBLIC, 0);
        cls.addOperation(op);
        model.addClass(cls);

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));
        when(repository.save(any())).thenReturn(new UmlModel(model.getId(), projectId, 6L));

        RemoveOperationCommand cmd = new RemoveOperationCommand("cmd1", "p1", projectId, classId, operationId, 5L);
        OperationRemovedEvent event = handler.handle(cmd);

        assertNotNull(event);
        assertEquals("OPERATION_REMOVED", event.eventType());
        assertEquals(classId, event.classId());
        assertEquals(operationId, event.operationId());
        assertEquals(6L, event.modelVersion());

        verify(publisher).publish(event);
        verify(repository).save(model);
        assertTrue(model.getClasses().get(0).getOperations().isEmpty());
    }

    @Test
    void testHandleVersionConflict() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 5L);

        when(repository.findByProjectId(projectId)).thenReturn(Optional.of(model));

        RemoveOperationCommand cmd = new RemoveOperationCommand("cmd1", "p1", projectId, UUID.randomUUID(), UUID.randomUUID(), 4L);
        assertThrows(com.umlcase.application.exception.ModelVersionConflictException.class, () -> handler.handle(cmd));

        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any(OperationRemovedEvent.class));
    }
}
