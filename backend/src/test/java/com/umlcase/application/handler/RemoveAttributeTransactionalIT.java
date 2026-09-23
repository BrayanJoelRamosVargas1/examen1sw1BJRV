package com.umlcase.application.handler;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class RemoveAttributeTransactionalIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("umlcase_test").withUsername("umlcase").withPassword("umlcase");

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired RemoveAttributeHandler handler;
    @Autowired UmlModelRepository repository;
    @Autowired JdbcTemplate jdbc;
    @MockBean SimpMessagingTemplate messagingTemplate;

    @Test
    void removesPostgresRowPreservesOtherIdsAndOrderAndBroadcastsAfterCommit() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("Cliente");
        model.addClass(umlClass);
        var first = model.addAttribute(umlClass.getId(), "a", "String", Visibility.PUBLIC);
        var middle = model.addAttribute(umlClass.getId(), "b", "String", Visibility.PUBLIC);
        var last = model.addAttribute(umlClass.getId(), "c", "String", Visibility.PUBLIC);
        long initialVersion = repository.save(model).getVersion();
        clearInvocations(messagingTemplate);

        var response = handler.handle(new UmlCommand.RemoveAttribute(
                UUID.randomUUID(), projectId, "p1", initialVersion, umlClass.getId(), middle.getId()));

        assertThat(response.modelVersion()).isEqualTo(initialVersion + 1);
        assertThat(response.attributeId()).isEqualTo(middle.getId());
        UmlModel reloaded = repository.findByProjectId(projectId).orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(initialVersion + 1);
        assertThat(reloaded.findClassById(umlClass.getId()).orElseThrow().getAttributes())
                .extracting(a -> a.getId()).containsExactly(first.getId(), last.getId());
        assertThat(reloaded.findClassById(umlClass.getId()).orElseThrow().getAttributes())
                .extracting(a -> a.getOrderIndex()).containsExactly(0, 2);
        assertThat(jdbc.queryForObject("select count(*) from uml_attributes where id = ?", Integer.class, middle.getId()))
                .isZero();
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/projects/" + projectId),
                (Object) argThat(event -> event instanceof com.umlcase.application.event.AttributeRemovedEvent removed
                        && removed.modelVersion() == response.modelVersion()
                        && removed.attributeId().equals(middle.getId())));
    }

    @Test
    void conflictDoesNotDeleteOrBroadcast() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass umlClass = UmlClass.create("ClienteConflicto");
        model.addClass(umlClass);
        var attribute = model.addAttribute(umlClass.getId(), "a", "String", Visibility.PUBLIC);
        long currentVersion = repository.save(model).getVersion();
        clearInvocations(messagingTemplate);

        assertThatThrownBy(() -> handler.handle(new UmlCommand.RemoveAttribute(
                UUID.randomUUID(), projectId, "p1", currentVersion - 1, umlClass.getId(), attribute.getId())))
                .isInstanceOf(com.umlcase.application.exception.ModelVersionConflictException.class);

        assertThat(jdbc.queryForObject("select count(*) from uml_attributes where id = ?", Integer.class, attribute.getId()))
                .isEqualTo(1);
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
