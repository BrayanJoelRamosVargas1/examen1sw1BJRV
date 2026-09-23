package com.umlcase.application.port.in;

import com.umlcase.domain.model.UmlModel;

public interface ImportModelUseCase {
    UmlModel importModel(ImportModelCommand command);
}
