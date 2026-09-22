package com.umlcase.infrastructure.web;

import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlOperation;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.web.dto.RemoveOperationRequest;
import com.umlcase.infrastructure.web.dto.RemoveOperationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RemoveOperationApiIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UmlModelRepository repository;

    @Test
    void testRemoveOperationSuccess() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 0L);
        UmlClass cls = new UmlClass(UUID.randomUUID(), "Test");
        UmlOperation op = new UmlOperation(UUID.randomUUID(), "testOp", "void", com.umlcase.domain.model.Visibility.PUBLIC, 0);
        cls.addOperation(op);
        model.addClass(cls);
        UmlModel saved = repository.save(model);

        String url = "/api/projects/" + projectId + "/classes/" + cls.getId() + "/operations/" + op.getId();
        RemoveOperationRequest req = new RemoveOperationRequest("cmd", "usr", saved.getVersion());

        ResponseEntity<RemoveOperationResponse> response = restTemplate.exchange(
                url, HttpMethod.DELETE, new HttpEntity<>(req), RemoveOperationResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(saved.getVersion() + 1, response.getBody().modelVersion());
    }
}
