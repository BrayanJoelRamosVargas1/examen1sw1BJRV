package com.umlcase.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlcase.application.ai.InterpretedUmlCommand;
import com.umlcase.application.ai.UmlAssistant;
import com.umlcase.application.ai.UmlAssistantResponse;
import com.umlcase.application.ai.UmlModelFinding;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.UmlModel;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AiUmlAssistant implements UmlAssistant {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiUmlAssistant(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public UmlAssistantResponse answer(String message, UmlModel model, List<UmlModelFinding> findings) {
        if (!properties.isEnabled() || properties.getBaseUrl().isBlank()) {
            return localAnswer(model, findings);
        }

        try {
            String context = compactContext(model, findings);
            String systemPrompt = "Eres un asistente de una herramienta CASE UML. Analiza únicamente el modelo proporcionado. "
                + "Distingue hechos observables de sugerencias. No inventes requisitos. No ejecutes comandos. "
                + "Si sugieres cambios, usa solo suggestedCommands con el esquema UML permitido. Nunca generes SQL, shell, rutas ni código ejecutable. "
                + "Responde JSON con answer, suggestedCommands y warnings.";
            Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", "MODELO_DATA:\n" + context + "\nPREGUNTA_DATA:\n" + message)
                ),
                "response_format", Map.of("type", "json_object")
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("El proveedor IA respondió HTTP " + response.statusCode());
            }
            var root = objectMapper.readTree(response.body());
            var content = root.path("choices").path(0).path("message").path("content").asText();
            return objectMapper.readValue(content, UmlAssistantResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La solicitud del asistente fue interrumpida", e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new IllegalStateException("Tiempo de espera agotado al consultar el asistente", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Respuesta inválida del asistente", e);
        }
    }

    private UmlAssistantResponse localAnswer(UmlModel model, List<UmlModelFinding> findings) {
        String answer = "Se analizaron " + model.getClasses().size() + " clases y "
            + model.getRelationships().size() + " relaciones. Se encontraron "
            + findings.size() + " observaciones.";
        return new UmlAssistantResponse(answer, findings, List.of(), List.of("Proveedor IA no configurado; análisis local aplicado."));
    }

    private String compactContext(UmlModel model, List<UmlModelFinding> findings) {
        String classes = model.getClasses().stream().map(this::compactClass).collect(Collectors.joining("; "));
        String relationships = model.getRelationships().stream()
            .map(relationship -> relationship.getType() + " " + relationship.getSourceClassId() + "->"
                + relationship.getTargetClassId() + " (" + relationship.getSourceMultiplicity() + ","
                + relationship.getTargetMultiplicity() + ")")
            .collect(Collectors.joining("; "));
        return "classes=[" + classes + "]; relationships=[" + relationships + "]; findings=" + findings;
    }

    private String compactClass(UmlClass umlClass) {
        String attributes = umlClass.getAttributes().stream()
            .map(attribute -> attribute.getName() + ":" + attribute.getType())
            .collect(Collectors.joining(","));
        String operations = umlClass.getOperations().stream()
            .map(operation -> operation.getName() + "()")
            .collect(Collectors.joining(","));
        return umlClass.getName() + "{" + attributes + "|" + operations + "}";
    }
}
