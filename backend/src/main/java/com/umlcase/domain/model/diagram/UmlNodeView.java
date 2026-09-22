package com.umlcase.domain.model.diagram;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class UmlNodeView {
    private String id;
    private String classId;
    
    @Setter
    private double x;
    
    @Setter
    private double y;
}
