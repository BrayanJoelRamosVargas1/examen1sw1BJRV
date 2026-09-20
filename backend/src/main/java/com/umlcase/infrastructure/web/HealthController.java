package com.umlcase.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint de salud del sistema.
 *
 * GET /api/health → 200 OK
 *
 * Propósito en Fase 0:
 *  1. Verificar que Spring Boot arranca correctamente.
 *  2. Verificar conectividad básica desde el frontend Angular.
 *  3. Evidencia de que el servidor está en funcionamiento.
 *
 * En fases posteriores podría extenderse con:
 *  - estado de la conexión a PostgreSQL
 *  - versión del sistema
 *  - estado de servicios dependientes
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Health check endpoint.
     *
     * @return 200 OK con timestamp y estado "UP"
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "uml-case-backend",
                "timestamp", Instant.now().toString(),
                "phase", "FASE-0"
        ));
    }
}
