package com.umlcase.infrastructure.web;

import com.umlcase.domain.model.RelationshipType;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.UmlRelationship;
import com.umlcase.domain.port.UmlModelRepository;
import com.umlcase.infrastructure.persistence.entity.JpaUmlModelEntity;
import com.umlcase.infrastructure.persistence.repository.SpringDataUmlModelRepository;
import com.umlcase.infrastructure.web.dto.UpdateRelationshipRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.umlcase.application.command.UmlCommand;
import com.umlcase.application.event.RelationshipUpdatedEvent;
import com.umlcase.application.port.in.UpdateRelationshipUseCase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UpdateRelationshipTransactionalIT {

    @Autowired
    private UpdateRelationshipUseCase updateRelationshipUseCase;

    @Autowired
    private UmlModelRepository repository;

    @Autowired
    private SpringDataUmlModelRepository springDataRepository;

    @Test
    @Transactional
    void updateRelationship_shouldIncrementModelVersionExactlyOnce() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazzA = UmlClass.create("ClassA");
        UmlClass clazzB = UmlClass.create("ClassB");
        model.addClass(clazzA);
        model.addClass(clazzB);
        UmlRelationship rel = UmlRelationship.create(RelationshipType.ASSOCIATION, clazzA.getId(), clazzB.getId());
        model.addRelationship(rel);

        UmlModel savedModel = repository.save(model);
        long initialVersion = savedModel.getVersion(); // Should be 0 initially

        var command = new UmlCommand.UpdateRelationship(
                UUID.randomUUID(),
                projectId,
                "browser-1",
                initialVersion,
                rel.getId(),
                RelationshipType.AGGREGATION,
                "1",
                "1..*"
        );

        // Act
        RelationshipUpdatedEvent event = updateRelationshipUseCase.handle(command);

        // Assert Application Layer (Event)
        assertThat(event.modelVersion()).isEqualTo(initialVersion + 1);
        assertThat(event.type()).isEqualTo(RelationshipType.AGGREGATION);

        // Assert Database Layer directly avoiding cache (though we are in transaction, let's flush)
        springDataRepository.flush();

        JpaUmlModelEntity dbEntity = springDataRepository.findByProjectId(projectId).orElseThrow();
        assertThat(dbEntity.getVersion()).isEqualTo(initialVersion + 1);
        
        var dbRel = dbEntity.getRelationships().iterator().next();
        assertThat(dbRel.getType()).isEqualTo("AGGREGATION");
        assertThat(dbRel.getSourceMultiplicity()).isEqualTo("1");
        assertThat(dbRel.getTargetMultiplicity()).isEqualTo("1..*");
    }
}
