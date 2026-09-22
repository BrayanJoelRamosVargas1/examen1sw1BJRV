package com.umlcase.application.handler;

import com.umlcase.application.command.AddOperationCommand;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AddOperationTransactionalIT {

    @Autowired
    private UmlModelRepository repository;

    @Autowired
    private AddOperationHandler handler;

    @Test
    void shouldAddOperationAndPersistParameters() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazz = UmlClass.create("Invoice");
        model.addClass(clazz);
        UmlModel saved = repository.save(model);

        AddOperationCommand cmd = AddOperationCommand.builder()
                .commandId(UUID.randomUUID())
                .projectId(projectId)
                .participantId("test-1")
                .expectedVersion(saved.getVersion())
                .classId(clazz.getId())
                .name("print")
                .returnType("void")
                .visibility(Visibility.PUBLIC)
                .parameters(List.of(
                        AddOperationCommand.ParameterData.builder().name("copies").type("int").build()
                ))
                .build();

        handler.handle(cmd);

        UmlModel fetched = repository.findByProjectId(projectId).orElseThrow();
        assertEquals(saved.getVersion() + 1, fetched.getVersion());
        UmlClass fetchedClass = fetched.getClasses().stream().filter(c -> c.getId().equals(clazz.getId())).findFirst().orElseThrow();
        assertEquals(1, fetchedClass.getOperations().size());
        assertEquals("print", fetchedClass.getOperations().get(0).getName());
        assertEquals(1, fetchedClass.getOperations().get(0).getParameters().size());
        assertEquals("copies", fetchedClass.getOperations().get(0).getParameters().get(0).getName());
    }
}
