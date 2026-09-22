# FASE 4.1 — CONFLICTO CONCURRENTE REAL A/B

## OBJETIVO
Probar y documentar el mecanismo de concurrencia optimista existente y endurecer las protecciones del frontend frente a peticiones concurrentes y eventos desordenados, garantizando la resolución transparente de conflictos (HTTP 409).

## ESTRATEGIA DE CONCURRENCIA

*   **Peticiones HTTP**: Todas las peticiones mutables incluyen un `expectedVersion`.
*   **PostgreSQL / JPA**: Se utiliza `@Version` (`modelVersion`) en la entidad raíz `UmlModel`.
*   **Protección Optimista**: Si `expectedVersion` de la petición es menor que la versión en BD, se lanza una `ModelVersionConflictException`.
*   **HTTP 409 Estructurado**: El manejador global de excepciones traduce el error a un código HTTP 409 con el payload estructurado `{"code":"MODEL_VERSION_CONFLICT","message":"..."}`.

## POLÍTICA DE RESOLUCIÓN

*   **First-Wins**: La primera mutación que llega con la versión esperada correcta incrementa la versión global de `N` a `N+1` y se persiste.
*   **Second Stale**: La segunda petición, portando `expectedVersion=N`, es rechazada por el backend (HTTP 409). **No se emiten eventos STOMP** para las peticiones rechazadas y la BD mantiene el estado consistente (`N+1`).

## RESPUESTA DEL FRONTEND (RESYNC AUTOMÁTICO)

1.  **Rechazo HTTP 409**: El componente intercepta el error en el flujo del observer.
2.  **GET /model Automático**: Ante un error 409, el frontend ejecuta inmediatamente la recarga del estado actual (resincronización completa).
3.  **Estado final**: El usuario perdedor observa en tiempo real los cambios del usuario ganador sin necesidad de pulsar F5 manualmente ni experimentar *lost updates*.

## PROTECCIONES ADICIONALES IMPLEMENTADAS (FRONTEND)

*   **Stale HTTP Response**: Si un POST/PATCH devuelve una `modelVersion < currentVersion` (ej. carreras de red o delays artificiales), se ignora el payload HTTP a favor de la verdad provista por STOMP.
*   **Stale GET**: Si un refresco (`GET /model`) trae una versión anterior a la que ya conocemos vía STOMP, se ignora.
*   **STOMP Out-of-Order**: 
    *   Si `event.version <= currentVersion`: El evento es atrasado/duplicado → Ignorar.
    *   Si `event.version == currentVersion + 1`: Incremental válido → Aplicar cambio parcial.
    *   Si `event.version > currentVersion + 1`: Salto de versión (pérdida de eventos) → Disparar `GET /model` para resincronización total.

## REGLAS ARQUITECTÓNICAS (MANTENIDAS)

*   NO se utiliza bloqueo pesimista (Pessimistic Locking).
*   NO se realiza `SELECT FOR UPDATE` en base de datos.
*   NO se usan bloques `synchronized` globales en la aplicación.
*   NO se manipula manualmente la versión del modelo (se confía enteramente en `@Version` de JPA).

## EVIDENCIA END-TO-END (Playwright Real A/B Test)

Se construyó un harness automatizado de Playwright interceptando el tráfico WebSocket para aislar y demostrar el conflicto:

```text
INITIAL VERSION: 62

A SUCCESS: (First Wins)
62 → 63

B STALE REQUEST: (Conflicto provocado bloqueando WS en B)
409 Conflict
code=MODEL_VERSION_CONFLICT

B AUTO RESYNC GET /model:
OK (Ejecutado automáticamente tras interceptar 409)

B FINAL VERSION:
63

DB VERSION:
63

ClienteGanador persisted: true
ClientePerdedor persisted: false
LOST UPDATE: false

RESULT: PASS
```
