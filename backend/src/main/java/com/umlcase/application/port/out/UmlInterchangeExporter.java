package com.umlcase.application.port.out;

import com.umlcase.domain.model.UmlModel;

public interface UmlInterchangeExporter {
    byte[] export(UmlModel model);
}
