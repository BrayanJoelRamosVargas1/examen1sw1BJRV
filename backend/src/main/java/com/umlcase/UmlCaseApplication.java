package com.umlcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la herramienta CASE.
 *
 * Backend implementado en Spring Boot 3.2.x.
 * Versión objetivo de Java: 21 LTS (esperando actualización de entorno).
 * Entorno temporal actual: Java 17 (ver ADR-003).
 */
@SpringBootApplication
public class UmlCaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmlCaseApplication.class, args);
    }


}
