# Fase 2.3.2: Update Operation

## Objetivo
Implementar la edición colaborativa de operaciones existentes, permitiendo modificar sus campos escalares (nombre, tipo de retorno, visibilidad) y sus parámetros en una sola mutación, preservando su identidad (`operationId`) y su índice de orden (`operation.orderIndex`). Los parámetros se tratan como parte integral de la operación (sin comandos CRUD independientes para parámetros).

## Implementación

### Backend

- **Endpoint REST:**
  - `PUT /api/projects/{projectId}/classes/{classId}/operations/{operationId}`
- **Comando y Controlador:**
  - `UpdateOperationCommand` procesado por `UpdateOperationHandler`
- **Modelo de Dominio y Persistencia:**
  - `UmlModel.updateOperation(...)`
  - Se modificó la relación `Operation` -> `Parameter` a un mapeo bidireccional (`mappedBy="operation"`) en JPA para evitar dobles flush/incrementos innecesarios (bug `N -> N+2`).
  - Se actualizó el mecanismo `markModified()` en `JpaUmlModelEntity` para garantizar un incremento monotónico incluso si la edición ocurre en el mismo milisegundo, asegurando que Hibernate siempre detecte la entidad raíz como modificada.
- **Eventos:**
  - Se lanza `OperationUpdatedEvent` (con `eventType = "OPERATION_UPDATED"`).
  - Publicado por `SpringUmlEventPublisher` y capturado por `StompUmlEventListener` (en la fase `AFTER_COMMIT`).

### Frontend
- Angular `UmlService.updateOperation(...)`
- El backend responde con `UpdateOperationResponse`, el cual contiene la `modelVersion` real final tras la operación.
- Sincronización a través del WebSocket donde se procesa `OPERATION_UPDATED` y se actualizan los datos en tiempo real sólo si `event.modelVersion == currentVersion + 1`.
- Se añadieron cabeceras HTTP `Cache-Control: no-cache, no-store, must-revalidate` al endpoint `GET /model` para asegurar que el navegador obtenga el estado fresco en caso de un soft-reload (F5).

### Prevención de Conflictos y Concurrencia
- Se usa JPA `@Version` para el control de versiones transaccional (Optimistic Locking).
- Se envía la versión esperada (`expectedVersion`) desde el frontend; si es diferente a la de la BD, lanza `ModelVersionConflictException`.
- **NO** se usó PESSIMISTIC_WRITE.
- **NO** se usó `SELECT FOR UPDATE`.
- **NO** se usó versionado manual fuera de Hibernate/JPA.
- **NO** se ejecutaron migraciones V5.

## Resolución de Bugs Críticos (Post-mortem)

Durante la implementación, se descubrieron y corrigieron bugs críticos de concurrencia/sincronización:
1. **Doble Flush de Hibernate:** El mapping original unidireccional de parámetros forzaba a Hibernate a insertar, actualizar dependencias y actualizar la raíz en flushes separados, resultando en saltos de versión `N -> N+2`. Corregido migrando a mapeo bidireccional puro.
2. **Colisiones de Dirty Checking:** `markModified()` usaba `System.currentTimeMillis()`. Si el evento ocurría en el mismo milisegundo (o la resolución del reloj del SO era insuficiente), `lastModified` no cambiaba, lo que anulaba el update de Hibernate y generaba que el endpoint devolviera la versión `N` antigua, provocando que los clientes STOMP ignorasen el evento. Corregido con avance matemático forzado en `lastModified`.
3. **Caché en el navegador:** Un `F5` devolvía a veces la versión antigua `40` en vez de la nueva persistida `41` en navegadores como Chrome debido a caché en `GET /model`. Corregido añadiendo encabezados no-cache.
4. **Validación STOMP Real:** Se agregó la prueba `UpdateOperationStompIT` con `@MockBean` a `SimpMessagingTemplate` que valida exhaustivamente el flujo completo (Commit DB -> Disparo AFTER_COMMIT -> Payload a `convertAndSend`).

## Evidencia y Pruebas
Se actualizó la batería de pruebas (84 tests totales).
- Cambios puramente escalares: Versión avanza estrictamente de `N -> N+1`.
- Cambios puramente en parámetros: Versión avanza estrictamente de `N -> N+1`.
- Cambios mixtos: Versión avanza estrictamente de `N -> N+1`.
- **Evidencia manual registrada:** `OPERATION_UPDATED "pedro" v45` recibido en ambas ventanas. Segunda mutación elevó la versión a `v46` y fue correctamente propagada, con `F5` restaurando el mismo estado consistentemente.
