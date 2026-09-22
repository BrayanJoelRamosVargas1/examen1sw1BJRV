package com.umlcase.application.handler;

import com.umlcase.application.command.RemoveOperationCommand;
import com.umlcase.application.event.OperationRemovedEvent;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RemoveOperationTransactionalIT {

    @Autowired
    private RemoveOperationHandler handler;

    @Autowired
    private UmlModelRepository repository;

    @Test
    void testHandleRemovesOperationAndUpdatesVersion() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 0L);
        UmlClass cls = new UmlClass(UUID.randomUUID(), "TestClass");
        UmlOperation op = new UmlOperation(UUID.randomUUID(), "op1", "void", com.umlcase.domain.model.Visibility.PUBLIC, 0);
        op.addParameter(new com.umlcase.domain.model.UmlParameter(UUID.randomUUID(), "p1", "String", 0));
        cls.addOperation(op);
        model.addClass(cls);

        UmlModel saved = repository.save(model);
        Long initialVersion = saved.getVersion();

        RemoveOperationCommand cmd = new RemoveOperationCommand(
                "cmd-1", "user-1", projectId, cls.getId(), op.getId(), initialVersion
        );

        OperationRemovedEvent event = handler.handle(cmd);

        assertEquals("OPERATION_REMOVED", event.eventType());
        assertEquals(initialVersion + 1, event.modelVersion());

        UmlModel updated = repository.findByProjectId(projectId).orElseThrow();
        assertEquals(initialVersion + 1, updated.getVersion());
        assertTrue(updated.getClasses().get(0).getOperations().isEmpty());
    }
}
