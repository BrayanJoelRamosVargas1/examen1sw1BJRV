package com.umlcase.application.generator.springboot;

import org.springframework.stereotype.Component;

@Component
public class ExceptionHandlerGenerator {

    public String generateException(String basePackage) {
        return "package " + basePackage + ".exception;\n\n" +
               "import org.springframework.http.HttpStatus;\n" +
               "import org.springframework.web.bind.annotation.ResponseStatus;\n\n" +
               "@ResponseStatus(HttpStatus.NOT_FOUND)\n" +
               "public class ResourceNotFoundException extends RuntimeException {\n" +
               "    public ResourceNotFoundException(String message) {\n" +
               "        super(message);\n" +
               "    }\n" +
               "}\n";
    }
    
    public String generateGlobalHandler(String basePackage) {
        return "package " + basePackage + ".exception;\n\n" +
               "import org.springframework.http.HttpStatus;\n" +
               "import org.springframework.http.ResponseEntity;\n" +
               "import org.springframework.web.bind.annotation.ControllerAdvice;\n" +
               "import org.springframework.web.bind.annotation.ExceptionHandler;\n\n" +
               "@ControllerAdvice\n" +
               "public class GlobalExceptionHandler {\n\n" +
               "    @ExceptionHandler(ResourceNotFoundException.class)\n" +
               "    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {\n" +
               "        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());\n" +
               "    }\n" +
               "}\n";
    }
}
