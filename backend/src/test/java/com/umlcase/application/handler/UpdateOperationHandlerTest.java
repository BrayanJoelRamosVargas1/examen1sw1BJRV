package com.umlcase.application.handler;

import com.umlcase.application.command.UpdateOperationCommand;
import com.umlcase.application.event.OperationUpdatedEvent;
import com.umlcase.application.port.out.UmlEventPublisher;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.model.UmlParameter;
import com.umlcase.domain.model.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateOperationHandlerTest {

    private UmlModelRepository repository;
    private UmlEventPublisher eventPublisher;
    private UpdateOperationHandler handler;

    private UmlModel model;
    private UmlClass cls;
    private UmlOperation operation;
    private UmlParameter p1;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        repository = mock(UmlModelRepository.class);
        eventPublisher = mock(UmlEventPublisher.class);
        handler = new UpdateOperationHandler(repository, eventPublisher);

        projectId = UUID.randomUUID();
        model = UmlModel.create(projectId);
        cls = UmlClass.create("Cliente");
        operation = UmlOperation.create("calcularTotal", "Double", Visibility.PUBLIC);
        p1 = UmlParameter.create("monto", "Double");
        operation.addParameter(p1);
        cls.addOperation(operation);
        model.addClass(cls);
        when(repository.loadForUpdate(projectId)).thenReturn(Optional.of(model));
        when(repository.save(any(UmlModel.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void shouldUpdateOperationSuccessfully() {
        var command = new UpdateOperationCommand(
                UUID.randomUUID(), projectId, "user1", 0, cls.getId(), operation.getId(),
                "calcularTotalDescuento", "BigDecimal", Visibility.PRIVATE,
                List.of(new UpdateOperationCommand.ParameterData(p1.getId(), "montoTotal", "BigDecimal"))
        );

        var event = handler.handle(command);

        verify(repository).save(model);
        verify(eventPublisher).publish(any(OperationUpdatedEvent.class));

        var op = model.getClasses().get(0).getOperations().get(0);
        assertThat(op.getName()).isEqualTo("calcularTotalDescuento");
        assertThat(op.getReturnType()).isEqualTo("BigDecimal");
        assertThat(op.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(op.getParameters()).hasSize(1);
        assertThat(op.getParameters().get(0).getName()).isEqualTo("montoTotal");

        assertThat(event.eventType()).isEqualTo("OPERATION_UPDATED");
        assertThat(event.name()).isEqualTo("calcularTotalDescuento");
        assertThat(event.parameters().get(0).name()).isEqualTo("montoTotal");
    }

    @Test
    void shouldThrowIfSignatureCollides() {
        var op2 = UmlOperation.create("buscar", "String", Visibility.PUBLIC);
        op2.addParameter(UmlParameter.create("codigo", "String"));
        cls.addOperation(op2);

        var command = new UpdateOperationCommand(
                UUID.randomUUID(), projectId, "user1", 0, cls.getId(), operation.getId(),
                "buscar", "Void", Visibility.PUBLIC,
                List.of(new UpdateOperationCommand.ParameterData(null, "codigo", "String"))
        );

        assertThrows(IllegalArgumentException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any(OperationUpdatedEvent.class));
    }

    @Test
    void shouldThrowIfVersionStale() {
        var command = new UpdateOperationCommand(
                UUID.randomUUID(), projectId, "user1", 1, cls.getId(), operation.getId(),
                "calcularTotalDescuento", "BigDecimal", Visibility.PRIVATE, List.of()
        );

        assertThrows(com.umlcase.application.exception.ModelVersionConflictException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }
}
