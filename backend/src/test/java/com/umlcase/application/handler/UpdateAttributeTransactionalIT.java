package com.umlcase.application.handler;

import com.umlcase.application.command.UpdateAttributeCommand;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.model.Visibility;
import com.umlcase.domain.model.UmlAttribute;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UpdateAttributeTransactionalIT {

    @Autowired
    private UpdateAttributeHandler handler;

    @Autowired
    private UmlModelRepository repository;

    @Test
    @Transactional
    void updateAttribute_savesCorrectly() {
        UUID projectId = UUID.randomUUID();
        UmlModel model = UmlModel.create(projectId);
        UmlClass clazz = UmlClass.create("User");
        model.addClass(clazz);
        UmlAttribute attr = model.addAttribute(clazz.getId(), "oldName", "String", Visibility.PRIVATE);
        
        UmlModel savedModel = repository.save(model);

        UpdateAttributeCommand cmd = UpdateAttributeCommand.builder()
                .commandId(UUID.randomUUID())
                .projectId(projectId)
                .participantId("p1")
                .expectedVersion(savedModel.getVersion())
                .classId(clazz.getId())
                .attributeId(attr.getId())
                .name("newName")
                .type("Integer")
                .visibility(Visibility.PUBLIC)
                .build();

        handler.handle(cmd);

        savedModel = repository.findByProjectId(projectId).orElseThrow();
        UmlAttribute updatedAttr = savedModel.findClassById(clazz.getId()).orElseThrow()
                .getAttributes().stream()
                .filter(a -> a.getId().equals(attr.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(updatedAttr.getName()).isEqualTo("newName");
        assertThat(updatedAttr.getType()).isEqualTo("Integer");
        assertThat(updatedAttr.getVisibility()).isEqualTo(Visibility.PUBLIC);
    }
}
