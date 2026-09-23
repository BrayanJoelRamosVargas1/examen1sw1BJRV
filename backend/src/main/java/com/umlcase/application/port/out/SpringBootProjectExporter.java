package com.umlcase.application.port.out;

import com.umlcase.application.port.in.GeneratedProject;
import com.umlcase.domain.generation.java.GeneratedJavaModel;

public interface SpringBootProjectExporter {

    GeneratedProject exportToZip(GeneratedJavaModel javaModel, byte[] flywaySql);
}
