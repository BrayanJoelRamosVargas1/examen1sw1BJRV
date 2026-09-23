# Fase 10.0: Asistente IA contextual del modelo UML

## Flujo

El asistente analiza el modelo actual sin modificarlo:

```text
pregunta -> snapshot UML -> analisis determinista -> IA opcional -> respuesta/findings/sugerencias -> preview -> decision del usuario
```

El endpoint `POST /api/projects/{projectId}/ai/assistant` recibe `{ "message": "..." }` y devuelve `answer`, `findings`, `suggestedCommands` y `warnings`. No usa `expectedVersion`, no guarda, no incrementa `modelVersion` y no publica STOMP.

## Analisis determinista

`UmlModelAnalyzer` funciona localmente y detecta únicamente hechos observables:

- `EMPTY_MODEL`
- `CLASS_WITHOUT_MEMBERS`
- `ISOLATED_CLASS`
- `ATTRIBUTE_WITH_UNSUPPORTED_GENERATION_TYPE`
- `RELATIONSHIP_WITHOUT_MULTIPLICITY`

Los findings contienen código, severidad, mensaje y elemento opcional. Una clase sin operaciones no es un error por sí sola; una clase sin miembros se informa como `INFO`.

## IA opcional y seguridad

`UmlAssistant` es el puerto de aplicación y `AiUmlAssistant` reutiliza `AiProperties` y el proveedor compatible existente. Con `AI_ENABLED=false`, devuelve una respuesta local con conteos y findings, sin requerir API key. Cuando está habilitada, envía solo un contexto semántico compacto: clases, miembros, relaciones y findings. No envía NodeViews, coordenadas, SQL, JPA, rutas ni logs.

La pregunta se trata como DATA dentro del prompt defensivo. La respuesta debe ser JSON estructurado. `answer` tiene máximo 4000 caracteres y `suggestedCommands` máximo 10. Las sugerencias se validan con la whitelist de 8.1 y las referencias deben apuntar a clases existentes o creadas antes en el mismo lote.

## Frontend

El panel `Asistente UML` muestra respuesta, observaciones, avisos y preview de cambios. `Descartar` no ejecuta mutaciones. `Aplicar sugerencias` reutiliza exactamente `executeAiCommandsBatch`, por lo que conserva `expectedVersion` secuencial, stop-on-409 y resincronización.

## Evidencia

Se validaron el analizador, el contrato de respuesta, el caso de uso, el endpoint HTTP read-only y el panel Angular. La integración usa fake provider en tests; no se consumió un proveedor IA real.

**Proveedor IA real: PENDIENTE.**
