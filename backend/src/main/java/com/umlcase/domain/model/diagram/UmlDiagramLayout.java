package com.umlcase.domain.model.diagram;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Builder
public class UmlDiagramLayout {
    private String projectId;
    
    @Setter
    private long version;
    
    private List<UmlNodeView> nodeViews;
    
    public void upsertNodeView(String classId, double x, double y) {
        Optional<UmlNodeView> existing = nodeViews.stream()
                .filter(n -> n.getClassId().equals(classId))
                .findFirst();
                
        if (existing.isPresent()) {
            existing.get().setX(x);
            existing.get().setY(y);
        } else {
            nodeViews.add(UmlNodeView.builder()
                    .id(UUID.randomUUID().toString())
                    .classId(classId)
                    .x(x)
                    .y(y)
                    .build());
        }
    }
}
