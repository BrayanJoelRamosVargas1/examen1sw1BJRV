package com.umlcase.application.ai;

import com.umlcase.domain.model.UmlModel;

public interface UmlImageInterpreter {
    UmlImageInterpretation interpret(byte[] image, String mimeType, UmlModel contextModel);
}
