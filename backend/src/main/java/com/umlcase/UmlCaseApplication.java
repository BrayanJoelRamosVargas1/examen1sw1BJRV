package com.umlcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la herramienta CASE.
 *
 * [DECISIÓN DE DISEÑO] Backend implementado en Spring Boot 3.x con Java 17.
 * Ver ADR-003.
 */
@SpringBootApplication
public class UmlCaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmlCaseApplication.class, args);
    }
}
