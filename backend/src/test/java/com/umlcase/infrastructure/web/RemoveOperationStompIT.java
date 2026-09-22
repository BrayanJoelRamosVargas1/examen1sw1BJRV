package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RemoveOperationRequest;
import com.umlcase.infrastructure.web.dto.RemoveOperationResponse;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RemoveOperationStompIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UmlModelRepository repository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRemoveOperationPublishesStompEvent() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 0L);
        UmlClass cls = new UmlClass(UUID.randomUUID(), "Test");
        UmlOperation op = new UmlOperation(UUID.randomUUID(), "testOp", "void", com.umlcase.domain.model.Visibility.PUBLIC, 0);
        cls.addOperation(op);
        model.addClass(cls);
        UmlModel saved = repository.save(model);

        String url = "/api/projects/" + projectId + "/classes/" + cls.getId() + "/operations/" + op.getId();
        RemoveOperationRequest req = new RemoveOperationRequest("cmd-1", "user-1", saved.getVersion());

        ResponseEntity<RemoveOperationResponse> response = restTemplate.exchange(
                url, HttpMethod.DELETE, new HttpEntity<>(req), RemoveOperationResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(saved.getVersion() + 1, response.getBody().modelVersion());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, timeout(5000)).convertAndSend(
                eq("/topic/projects/" + projectId),
                payloadCaptor.capture()
        );

        Object eventPayload = payloadCaptor.getValue();
        String jsonEvent = objectMapper.writeValueAsString(eventPayload);

        System.out.println("STOMP PAYLOAD SENT:");
        System.out.println(jsonEvent);

        com.umlcase.application.event.OperationRemovedEvent event =
                objectMapper.readValue(jsonEvent, com.umlcase.application.event.OperationRemovedEvent.class);

        assertEquals("OPERATION_REMOVED", event.eventType());
        assertEquals(saved.getVersion() + 1, event.modelVersion());
        assertEquals(op.getId(), event.operationId());
    }
}
