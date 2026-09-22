package com.umlcase.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.application.event.NodeMovedEvent;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.infrastructure.persistence.repository.SpringDataUmlModelRepository;
import com.umlcase.infrastructure.persistence.repository.diagram.JpaUmlDiagramLayoutRepository;
import com.umlcase.infrastructure.web.dto.diagram.SaveNodeViewRequest;
import com.umlcase.infrastructure.web.dto.diagram.SaveNodeViewResponse;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NodeMovedStompIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpringDataUmlModelRepository modelRepository;

    @Autowired
    private JpaUmlDiagramLayoutRepository layoutRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        layoutRepository.deleteAll();
        modelRepository.deleteAll();
    }

    @Test
    void testMoveNodePublishesStompEvent() throws Exception {
        String projectId = "00000000-0000-0000-0000-000000000001";
        String classId = "dbd46dc0-76be-45b5-ab5e-b38e6ab52de9";

        com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity model = new com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity();
        model.setId(UUID.fromString(projectId));
        model.setProjectId(UUID.fromString(projectId));
        model.setVersion(42L);
        com.umlcase.infrastructure.persistence.entity.JpaUmlClassEntity cls = new com.umlcase.infrastructure.persistence.entity.JpaUmlClassEntity();
        cls.setId(UUID.fromString(classId));
        cls.setName("TestClass");
        model.getClasses().add(cls);
        modelRepository.saveAndFlush(model);

        com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity layoutEntity = new com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity();
        layoutEntity.setProjectId(UUID.fromString(projectId));
        layoutEntity.setVersion(10L);
        layoutEntity.setLastModified(java.time.Instant.now());
        layoutRepository.saveAndFlush(layoutEntity);

        String commandId = UUID.randomUUID().toString();
        SaveNodeViewRequest request = new SaveNodeViewRequest();
        request.setCommandId(commandId);
        request.setParticipantId("tester");
        request.setExpectedLayoutVersion(10L);
        request.setX(150.0);
        request.setY(250.0);

        String url = "/api/projects/" + projectId + "/diagram/nodes/" + classId;
        
        ResponseEntity<SaveNodeViewResponse> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(request),
                SaveNodeViewResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(11L, response.getBody().getLayoutVersion());
        assertEquals(150.0, response.getBody().getX());
        assertEquals(250.0, response.getBody().getY());

        ArgumentCaptor<NodeMovedEvent> eventCaptor = ArgumentCaptor.forClass(NodeMovedEvent.class);
        verify(messagingTemplate, timeout(5000).times(1)).convertAndSend(
                eq("/topic/projects/" + projectId + "/diagram"),
                eventCaptor.capture()
        );

        NodeMovedEvent publishedEvent = eventCaptor.getValue();
        assertEquals("NODE_MOVED", publishedEvent.getEventType());
        assertEquals(commandId, publishedEvent.getCommandId());
        assertEquals(projectId, publishedEvent.getProjectId());
        assertEquals(classId, publishedEvent.getClassId());
        assertEquals(150.0, publishedEvent.getX());
        assertEquals(250.0, publishedEvent.getY());
        assertEquals(11L, publishedEvent.getLayoutVersion());
    }

    @Test
    void testMoveNodeStaleVersionDoesNotPublishEvent() throws Exception {
        String projectId = "00000000-0000-0000-0000-000000000001";
        String classId = "dbd46dc0-76be-45b5-ab5e-b38e6ab52de9";

        com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity model = new com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity();
        model.setId(UUID.fromString(projectId));
        model.setProjectId(UUID.fromString(projectId));
        model.setVersion(42L);
        com.umlcase.infrastructure.persistence.entity.JpaUmlClassEntity cls = new com.umlcase.infrastructure.persistence.entity.JpaUmlClassEntity();
        cls.setId(UUID.fromString(classId));
        cls.setName("TestClass");
        model.getClasses().add(cls);
        modelRepository.saveAndFlush(model);

        com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity layoutEntity = new com.umlcase.infrastructure.persistence.entity.diagram.JpaUmlDiagramLayoutEntity();
        layoutEntity.setProjectId(UUID.fromString(projectId));
        layoutEntity.setVersion(10L);
        layoutEntity.setLastModified(java.time.Instant.now());
        layoutRepository.saveAndFlush(layoutEntity);

        SaveNodeViewRequest request = new SaveNodeViewRequest();
        request.setCommandId(UUID.randomUUID().toString());
        request.setParticipantId("tester");
        request.setExpectedLayoutVersion(9L); // Stale
        request.setX(150.0);
        request.setY(250.0);

        String url = "/api/projects/" + projectId + "/diagram/nodes/" + classId;

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(request),
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        
        // Ensure no events were published
        verifyNoInteractions(messagingTemplate);
    }
}
