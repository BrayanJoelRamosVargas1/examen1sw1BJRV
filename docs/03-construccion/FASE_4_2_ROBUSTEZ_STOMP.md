# Fase 4.2: Robustez STOMP (Duplicados, Stale Events y Gaps)

## Objetivo
Garantizar la resiliencia del cliente colaborativo (Frontend) ante problemas de red comunes en arquitecturas asíncronas pub/sub como WebSocket/STOMP, incluyendo la pérdida de paquetes (gaps), llegada de mensajes duplicados o desordenados, y el cruce de respuestas HTTP obsoletas con notificaciones STOMP frescas.

## Políticas de Secuencia Estricta Implementadas

La consistencia causal del cliente se mantiene observando el `modelVersion` de la aplicación versus el `modelVersion` del payload STOMP o la respuesta HTTP.

### 1. Duplicados y Eventos Stale (STOMP)
```typescript
event.version <= currentVersion -> IGNORE
```
Cualquier evento STOMP cuyo `modelVersion` sea igual o menor a la versión que ya tiene renderizada el cliente es ignorado silenciosamente. Esto resuelve rebotes de red y problemas de *stale events*.

### 2. Sincronización Incremental (STOMP)
```typescript
event.version == currentVersion + 1 -> APPLY incremental
```
Condición de flujo normal. El cliente avanza la versión y muta localmente el estado de acuerdo con el payload del evento, sin consultar al backend.

### 3. Recuperación Ante Gaps (STOMP)
```typescript
event.version > currentVersion + 1 -> GAP -> GET /model
```
Si el cliente detecta un salto de versión (ej: está en v87 y llega v89), asume que perdió el evento v88. El cliente rechaza aplicar v89 incrementalmente y emite automáticamente un `GET /model` al backend para reconstruir el estado íntegro y correcto de la pizarra colaborativa.

### 4. Protección Contra GET Viejo y Buffer Draining
```typescript
snapshot.version < currentVersion -> snapshot ignorado
```
Si un `GET /model` responde con una versión anterior a la actual (debido a retardos de proxy o de procesamiento), la respuesta HTTP es ignorada.
**IMPORTANTE (Bug fix):** Si existen eventos acumulados en el `eventBuffer` (eventos que entraron mientras el GET estaba pendiente), NO se descartan silenciosamente; se drenan mediante `applyEvent(...)` permitiendo que el estado avance correctamente si esos eventos son válidos.

### 5. Protección Contra HTTP Response Vieja (Mutaciones)
```typescript
response.modelVersion < currentVersion -> ignorar response
```
Si el cliente ejecuta una mutación REST (ej: `renameClass`) pero, antes de que llegue la respuesta 200 OK del REST, recibe el evento STOMP (ej. v51) emitido por esa misma mutación, el cliente se actualiza a v51. Cuando finalmente la promesa HTTP resuelve, si su payload indica que el estado de esa llamada era menor que la versión actual (ej. 50 < 51), se descarta, previniendo regresiones del estado.

## Evidencia de Validación (E2E y Unit Tests)

Se creó una suite exhaustiva `AppComponent (Robustez STOMP)` en Angular (Jasmine/Karma) que valida matemáticamente todas estas condiciones.

**Frontend Tests (6/6 SUCCESS):**
1. `duplicate events`
2. `stale events (delayed in network)`
3. `gap -> resync`
4. `stale GET`
5. `stale GET + buffered event`
6. `stale HTTP response`

**Evidencia de Recuperación de GAP Automática (Playwright E2E Final):**
```text
INITIAL VERSION: 91
INITIAL CLASSES: Renombrada2_1790118542991_mut2, GapRegr2_1790121594511, ClienteGanador_mut2, Mutacion2_1790120530848, Mutacion1
TARGET CLASS:    "Renombrada2_1790118542991_mut2"

A LOADED: version=91
B LOADED: version=91

A: Renaming "Renombrada2_1790118542991_mut2" → "Mutacion1" via REST...
EVENT 92 LOST BY B
A VERSION: 92
B VERSION: 91

A: Renaming "Mutacion1_1790121974868" → "Mutacion2" via REST...
EVENT 93 RECEIVED BY B
GAP DETECTED: true

B AUTO RESYNC GET /model: true

FINAL A: 93
FINAL B: 93
DB: 93
SAME STATE: true

RESULT: PASS
```

**Backend Tests:**
118 tests. 0 failures. 0 errors. 0 skipped.

*Nota: Ningún cambio fue necesario en el backend (`Hibernate`/`JPA`). El manejo de versiones concurrentes implementado en la Fase 4.1 y la infraestructura STOMP ya enviaba la información suficiente; el trabajo consistió exclusivamente en gobernar el Frontend para que procesara los eventos de manera tolerante a fallos.*
