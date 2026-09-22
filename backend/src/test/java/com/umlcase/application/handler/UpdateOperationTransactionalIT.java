package com.umlcase.application.handler;

import com.umlcase.application.command.UpdateOperationCommand;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.model.UmlParameter;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
class UpdateOperationTransactionalIT {

    @Autowired
    private UmlModelRepository repository;

    @Autowired
    private UpdateOperationHandler handler;

    private UUID projectId;
    private UmlClass cls;
    private UmlOperation operation;
    private UmlParameter param;
    private long savedModelVersion;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        cls = UmlClass.create("ClaseTransaccional");
        operation = UmlOperation.create("op1", "void", Visibility.PUBLIC);
        param = UmlParameter.create("p1", "String");
        operation.addParameter(param);
        cls.addOperation(operation);
        model.addClass(cls);
        savedModelVersion = repository.save(model).getVersion();
    }

    @Test
    void shouldPersistUpdateCorrectly() {
        var command = new UpdateOperationCommand(
                UUID.randomUUID(), projectId, "u1", savedModelVersion, cls.getId(), operation.getId(),
                "op2", "Integer", Visibility.PROTECTED,
                List.of(
                        new UpdateOperationCommand.ParameterData(param.getId(), "p1_renamed", "Integer"),
                        new UpdateOperationCommand.ParameterData(null, "p2_new", "String")
                )
        );

        var event = handler.handle(command);

        var savedModel = repository.findByProjectId(projectId).orElseThrow();
        var savedOp = savedModel.getClasses().get(0).getOperations().get(0);

        assertThat(savedModel.getVersion()).isEqualTo(savedModelVersion + 1);
        assertThat(savedOp.getName()).isEqualTo("op2");
        assertThat(savedOp.getReturnType()).isEqualTo("Integer");
        assertThat(savedOp.getVisibility()).isEqualTo(Visibility.PROTECTED);
        assertThat(savedOp.getParameters()).hasSize(2);

        // El ID antiguo se preserva
        assertThat(savedOp.getParameters().get(0).getId()).isEqualTo(param.getId());
        assertThat(savedOp.getParameters().get(0).getName()).isEqualTo("p1_renamed");

        // El nuevo ID se genera automáticamente
        assertThat(savedOp.getParameters().get(1).getName()).isEqualTo("p2_new");

        // El evento refleja el versionamiento correcto
        assertThat(event.modelVersion()).isEqualTo(savedModelVersion + 1);
    }
}
