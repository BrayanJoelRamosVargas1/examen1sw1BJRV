package com.umlcase.application.port.in.diagram;

import com.umlcase.application.query.diagram.GetDiagramLayoutQuery;
import com.umlcase.domain.model.diagram.UmlDiagramLayout;

public interface GetDiagramLayoutUseCase {
    UmlDiagramLayout execute(GetDiagramLayoutQuery query);
}
