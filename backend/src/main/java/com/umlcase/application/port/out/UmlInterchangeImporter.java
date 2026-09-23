package com.umlcase.application.port.out;

import com.umlcase.domain.model.UmlModel;

public interface UmlInterchangeImporter {
    UmlModel importModel(byte[] content);
}
