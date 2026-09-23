# Fase 9.0: Foto o imagen a modelo UML

## Flujo

La imagen se procesa como una entrada adicional al mismo pipeline estructurado de 8.1:

```text
imagen -> endpoint read-only -> UmlImageInterpreter -> validacion -> preview -> confirmacion -> batch REST existente
```

La interpretación no guarda archivos, no ejecuta mutaciones, no incrementa `modelVersion` y no publica STOMP. La confirmación reutiliza `executeAiCommandsBatch`, con `expectedVersion` secuencial y detención/resincronización ante `409`.

## Puertos y adaptador

`UmlImageInterpreter` y `InterpretUmlImageUseCase` pertenecen a la aplicación. `MultimodalUmlImageInterpreter` es un adaptador compatible con proveedores multimodales configurados mediante `AI_ENABLED`, `AI_BASE_URL`, `AI_API_KEY` y `AI_MODEL`. El dominio UML no conoce HTTP, base64, MIME ni OCR.

El contrato de salida reutiliza `InterpretedUmlCommand` y agrega `warnings`. Solo se aceptan comandos, tipos, relaciones y multiplicidades de la whitelist de 8.1. Las respuestas se validan antes de mostrarse.

## Archivos

El endpoint `POST /api/projects/{projectId}/ai/interpret-image` recibe `multipart/form-data` en el campo `file`. Se aceptan únicamente PNG, JPEG y WebP, con máximo 8 MB. El backend valida MIME, tamaño, archivo vacío y firmas binarias en memoria; SVG, PDF, ZIP, ejecutables y archivos renombrados son rechazados.

## Frontend

Angular muestra vista previa local, estado de análisis, comandos y warnings. Un archivo inválido no genera request. Los comandos no se ejecutan automáticamente: el usuario debe confirmar o cancelar.

## Evidencia

Los tests cubren firmas PNG/JPEG/WebP, límites, MIME inválido, contenido falso, caso de uso, multipart HTTP, `modelVersion` sin cambios, preview, warnings y cancelación. Se usa un fake de `UmlImageInterpreter`; no se consumió un proveedor multimodal real.

**Validación multimodal real: PENDIENTE.**
