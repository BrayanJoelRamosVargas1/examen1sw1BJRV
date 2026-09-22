package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.application.event.OperationUpdatedEvent;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.UmlParameterDto;
import com.umlcase.infrastructure.web.dto.UpdateOperationRequest;
import com.umlcase.infrastructure.web.dto.UpdateOperationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.show-sql=true"
})
@Testcontainers
public class UpdateOperationStompIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("umlcase_test")
            .withUsername("umlcase")
            .withPassword("umlcase");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UmlModelRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private UUID projectId;
    private UmlClass cls;
    private UmlOperation operation;
    private long initialVersion;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        cls = UmlClass.create("C1");

        operation = UmlOperation.create("procesarPedido", "void", Visibility.PUBLIC);
        cls.addOperation(operation);

        model.addClass(cls);
        initialVersion = repository.save(model).getVersion();
    }

    @Test
    void shouldPublishStompEventAfterCommit() throws Exception {
        var requestBody = new UpdateOperationRequest(
                UUID.randomUUID(), "u1", initialVersion, "calcularPedidoDefinitivo", "void", "PUBLIC",
                List.of()
        );

        String url = "/api/projects/" + projectId + "/classes/" + cls.getId() + "/operations/" + operation.getId();
        ResponseEntity<UpdateOperationResponse> responseEntity = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(requestBody), UpdateOperationResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        long httpModelVersion = responseEntity.getBody().modelVersion();

        ArgumentCaptor<OperationUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(OperationUpdatedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/projects/" + projectId), eventCaptor.capture());

        OperationUpdatedEvent event = eventCaptor.getValue();
        assertEquals("OPERATION_UPDATED", event.eventType());
        assertEquals("calcularPedidoDefinitivo", event.name());
        assertEquals(httpModelVersion, event.modelVersion());

        System.out.println("STOMP PAYLOAD SENT:");
        System.out.println(objectMapper.writeValueAsString(event));
    }
}
