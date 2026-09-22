package com.umlcase.infrastructure.web;

import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipRemovedEvent;
import com.umlcase.application.port.in.RemoveRelationshipUseCase;
import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RemoveRelationshipTransactionalIT {

    @Autowired
    private RemoveRelationshipUseCase useCase;

    @Autowired
    private UmlModelRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UmlModel model;
    private UmlRelationship relationship;

    @BeforeEach
    void setUp() {
        model = transactionTemplate.execute(status -> {
            UmlModel newModel = UmlModel.create(UUID.randomUUID());
            UmlClass class1 = new UmlClass(UUID.randomUUID(), "Source");
            UmlClass class2 = new UmlClass(UUID.randomUUID(), "Target");
            newModel.addClass(class1);
            newModel.addClass(class2);
            
            relationship = new UmlRelationship(
                    UUID.randomUUID(),
                    RelationshipType.ASSOCIATION,
                    class1.getId(),
                    class2.getId(),
                    "1",
                    "*"
            );
            newModel.addRelationship(relationship);
            return repository.save(newModel);
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.execute(status -> {
            repository.deleteById(model.getId());
            return null;
        });
    }

    @Test
    void shouldRemoveRelationshipPhysicallyAndCommit() {
        // Given
        UUID commandId = UUID.randomUUID();
        var command = new UmlCommand.RemoveRelationship(
                commandId,
                model.getProjectId(),
                "browser-A",
                model.getVersion(),
                relationship.getId()
        );

        // When
        RelationshipRemovedEvent event = transactionTemplate.execute(status -> useCase.handle(command));

        // Then (new transaction)
        transactionTemplate.execute(status -> {
            UmlModel dbModel = repository.findByProjectId(model.getProjectId()).orElseThrow();
            
            assertThat(dbModel.getVersion()).isEqualTo(model.getVersion() + 1);
            assertThat(event.modelVersion()).isEqualTo(model.getVersion() + 1);
            
            assertThat(dbModel.getRelationships()).isEmpty();
            assertThat(dbModel.getClasses()).hasSize(2);
            return null;
        });
    }
}
