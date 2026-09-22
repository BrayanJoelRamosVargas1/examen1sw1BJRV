# Fase 2.2.3: Remove Attribute (Colaborativo) y Estabilización

## Resumen Técnico
Se implementó la eliminación de atributos (`Remove Attribute`) manteniendo el mecanismo colaborativo y optimista. Adicionalmente, se introdujo una protección estructural en el cliente contra **condiciones de carrera (Race Conditions)** que afectaban a todas las mutaciones de la Fase 2.2 (Add, Update, Remove).

## Endpoint REST
- **Ruta**: `DELETE /api/projects/{projectId}/classes/{classId}/attributes/{attributeId}`
- **Request Body**: `RemoveAttributeRequest` (incluye `commandId`, `participantId` y `expectedVersion`).
- **Respuesta**: HTTP 200 OK con un JSON `RemoveAttributeResponse` que contiene el `classId`, el `removedAttributeId`, y el nuevo `modelVersion` (persistido).

## Backend
- **Flujo**: Controlador -> `RemoveAttributeHandler` -> `UmlModel.removeAttribute` -> `UmlClass.removeAttribute`.
- **Eliminación Lógica/Física**: Se elimina físicamente el atributo de la colección.
- **Indexación**: **No se compactan** los `orderIndex` de los atributos restantes tras una eliminación, dejando gaps naturales (ej. 0, 2) para preservar la estabilidad de índices.
- **Concurrencia**: Se utiliza Optimistic Locking mediante `@Version`. El cambio sube la versión de DB de N a N+1.
- **Eventos**: Se publica `AttributeRemovedEvent` con `eventType = ATTRIBUTE_REMOVED` durante la fase `AFTER_COMMIT` de la transacción.

## Frontend (Estabilización de Carrera - Race Condition Fix)
El cliente colaborativo fue endurecido para impedir que respuestas tardías asíncronas dañen el estado local:
1. **snapshot GET antiguo no puede sobrescribir estado más nuevo**:
   - Cada solicitud a `loadModel()` genera un `requestId` interno. Si al resolver la promesa/observable el `requestId` cambió (porque se solicitó un snapshot más nuevo), se descarta el resultado viejo.
   - Si el buffer de eventos WS se procesa y el snapshot no ha llegado, los eventos se quedan esperando.
2. **Respuestas HTTP tardías no pueden hacer retroceder `modelVersion`**:
   - Para las mutaciones locales (Add/Update/Remove), si el servidor responde con un `modelVersion < this.currentVersion`, la respuesta local es descartada (forzando un resync vía `loadModel()` si el estado difiere).
   - El estado local con versión más nueva **nunca debe ser reemplazado por una respuesta o snapshot más viejo**.
3. **Mecanismo de Deduplicación y Avance (STOMP)**:
   - `STOMP <= currentVersion` -> `ignore` (ya lo apliqué por mi propio HTTP o era un evento obsoleto).
   - `STOMP == currentVersion + 1` -> `apply` (lo aplico incrementalmente).
   - `STOMP > currentVersion + 1` -> `GET /model` (me perdí algo, resincronizo completo).

## Pruebas y Validación (Gate Superado)
- **DELETE** ejecutado con éxito, retornando HTTP 200 JSON con la versión exacta N+1.
- El cliente **A** aplica el cambio mediante la respuesta HTTP. El cliente **B** lo recibe vía STOMP (`ATTRIBUTE_REMOVED`) sin `F5`.
- **Persistencia garantizada**: Tras refrescar (`F5`) en A y B, la clase retiene las posiciones (ej. `Agua orderIndex=0`, `arboles orderIndex=2`) y el atributo eliminado no reaparece.
- **Suite automatizada**: 70/70 backend (Failures 0, Errors 0, Skipped 0), Angular compilado con SUCCESS, demostrando compatibilidad del endurecimiento de carreras.
