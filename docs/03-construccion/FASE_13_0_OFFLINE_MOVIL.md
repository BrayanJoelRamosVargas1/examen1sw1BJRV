# Fase 13.0 — Offline + Cola + Sincronización Móvil

## Objetivo

Permitir al usuario operar el cliente móvil Flutter sin conexión, encolando intenciones de cambio
de forma persistente, y sincronizarlas de forma segura y reproducible cuando la conectividad retorna.

Las reglas de negocio definitivas siempre se ejecutan en el **backend CASE**; el móvil solo cachea
y registra intenciones.

---

## Diagrama de estados

```
ONLINE
  ↓ pérdida de red / error de transporte
OFFLINE
  ↓ usuario registra cambios → cola persistente
QUEUED
  ↓ recupera conexión / "Sincronizar ahora"
SYNCING
  ├─ success → ONLINE (cola vacía, cache actualizado)
  └─ HTTP 409 / 400 → CONFLICT (cola preservada, estado visible en UI)
```

---

## Arquitectura de persistencia

### `OfflineStore` (abstracta)

Contrato para guardar/leer los cuatro tipos de dato local:

| Clave               | Tipo                         | Descripción                          |
|---------------------|------------------------------|--------------------------------------|
| `cachedModel`       | `UmlModel?`                  | Snapshot del servidor                |
| `pendingCommands`   | `List<PendingUmlCommand>`    | Cola de comandos pendientes          |
| `participantId`     | `String`                     | Identidad estable del dispositivo    |
| `localIdMap`        | `Map<String, String>`        | Mapeo local:uuid → server:uuid       |

### `SharedPreferencesOfflineStore`

Implementación MVP usando `shared_preferences`. Serialización JSON versionada
(`schemaVersion: 1`). No almacena nunca secrets, API keys ni passwords.

---

## Identidad del participante

### `ParticipantIdentityService.loadOrCreate()`

- Al primer arranque: genera `mobile-<uuid_v4>` y lo persiste.
- En cada arranque posterior: carga el mismo ID sin regenerarlo.
- **Resultado**: el `participantId` es estable para la vida útil del dispositivo.

---

## Modelo de comando pendiente

### `PendingUmlCommand`

```dart
class PendingUmlCommand {
  final String localId;     // Identificador de fila local (no enviado al servidor)
  final String commandId;   // UUID v4 generado UNA sola vez — no se regenera en replay
  final String type;        // 'CREATE_CLASS' | 'ADD_ATTRIBUTE'
  final Map<String, dynamic> payload;
  final DateTime createdAt;
  final int baseVersion;    // Versión del modelo cuando se encoló (informativo, no autoritativo)
  final PendingCommandStatus status; // pending | syncing | conflict | failed
}
```

El `commandId` es **inmutable** tras creación. El mismo ID se envía al backend en cada
reintento, garantizando idempotencia.

---

## Comandos soportados offline (MVP)

| Comando           | Soporte offline |
|-------------------|-----------------|
| `CREATE_CLASS`    | ✅ encolado      |
| `ADD_ATTRIBUTE`   | ✅ encolado      |
| `RENAME_CLASS`    | ❌ requiere conexión — UI lo indica |
| `DELETE_CLASS`    | ❌ requiere conexión |
| Mutaciones de relaciones | ❌ requiere conexión |
| Import XMI        | ❌ requiere conexión |

Esta es una decisión de alcance MVP, **no** una limitación ocultada. La UI muestra
`"Esta acción requiere conexión."` para las operaciones no soportadas offline.

---

## IDs locales y mapeo

Cuando el usuario crea una clase offline, se genera un **`tempClassId`**:

```
tempClassId = 'local:<uuid_v4>'
```

Este ID referencia a la clase dentro de la cola (p. ej., para un `ADD_ATTRIBUTE` sobre esa clase).
Al sincronizar, el `OfflineSyncService` mantiene `localToServerId`:

```
CREATE_CLASS (comandId=X, tempClassId=local:abc)
  → backend responde: classId=server-uuid-123
  → localToServerId['local:abc'] = 'server-uuid-123'

ADD_ATTRIBUTE (classRef=local:abc, ...)
  → se resuelve a server-uuid-123 antes de enviar
```

---

## Algoritmo de sincronización (`OfflineSyncService.sync()`)

```
1. Si queue vacía → GET /model → actualizar cache → estado=ONLINE → fin

2. GET /model servidor → serverVersion = v_actual

3. Por cada comando en cola (orden createdAt):

   a. Construir request:
        expectedVersion = serverVersion   ← versión del servidor, no baseVersion
        commandId = comandId del comando  ← idempotente, no se regenera

   b. Si éxito (2xx):
        serverVersion = response.modelVersion
        actualizar localToServerId si aplica
        remover comando de cola
        persistir cola y mapping

   c. Si HTTP 409 o 4xx:
        marcar comando como conflict/failed
        persistir cola
        GET /model → actualizar cache
        estado = CONFLICT
        detener replay

   d. Si error de transporte (SocketException, TimeoutException):
        estado = OFFLINE
        detener replay

4. GET /model final → cache actualizado → estado = ONLINE
```

---

## Rebase de intención

Si el servidor avanzó mientras el móvil estaba offline (e.g. `baseVersion=10`, `serverVersion=15`),
la sincronización **no** envía `expectedVersion=10`. Usa `serverVersion=15` actual.

Las reglas del backend deciden si las operaciones aditivas siguen siendo válidas.
Esto es un **rebase de intención**: el cliente re-aplica sus cambios sobre la versión actual del
servidor, sin sobrescribir los cambios intermedios.

---

## Comportamiento ante conflictos

| Caso                        | Comportamiento                                               |
|-----------------------------|--------------------------------------------------------------|
| HTTP 409 durante replay     | Parar. Marcar primer comando como `conflict`. Estado=CONFLICT. Cola preservada. |
| HTTP 400 durante replay     | Parar. Marcar comando como `failed`. Estado=CONFLICT. Cola preservada. |
| Error de transporte durante replay | Parar. Estado=OFFLINE. Cola pendiente sin marcar como failed. |

**No se borra la cola silenciosamente. No se regeneran commandIds. No se hace loop infinito.**

UI muestra:
> "El modelo cambió durante la sincronización. Tus cambios pendientes se conservaron."

---

## UI de estado

Barra superior:

| Estado    | Indicador                                         |
|-----------|---------------------------------------------------|
| Online    | 🟢 Online                                         |
| Syncing   | 🟠 Sincronizando...                               |
| Offline   | 🔴 Offline — N cambios pendientes                 |
| Conflict  | ⚠ Conflicto — N cambios pendientes               |

Clases pendientes muestran badge `pendiente`. El panel de cola lista:

```
Cambios pendientes (2)
• Crear clase "Factura"   [pendiente]
• Agregar total:Decimal a local:abc  [pendiente]
```

Botón **Sincronizar ahora** disponible siempre en AppBar.

---

## STOMP y offline

Al estar offline, STOMP no está conectado. Al reconectar:

```
1. GET snapshot servidor
2. sync cola
3. GET snapshot final
4. conectar/reconectar STOMP
```

Política de eventos STOMP:

```
event.version <= current     → ignorar (duplicado)
event.version == current + 1 → aplicar
event.version > current + 1  → GET /model (gap detectado)
```

---

## Humo manual de integración

Para validar contra el backend real sin automatizar en CI:

```bash
# 1. Levantar backend
cd backend && ./mvnw spring-boot:run

# 2. En móvil (simulador/dispositivo):
#    - Abrir app
#    - Verificar 🟢 Online
#    - Activar modo avión
#    - Crear clase "FacturaOffline" → debe aparecer con badge [pendiente]
#    - Agregar atributo "total:Decimal" a FacturaOffline → segunda entrada en cola
#    - Desactivar modo avión
#    - Pulsar "Sincronizar ahora"
#    - Verificar 🟠 → 🟢, cola vacía, clases sin badge
#    - Verificar en Angular que FacturaOffline:total existe en el modelo
```

---

## Tests (40/40)

| Grupo                                 | Tests |
|---------------------------------------|-------|
| PendingUmlCommand serialization       | 3     |
| Cache model save/load                 | 4     |
| ParticipantIdentityService            | 3     |
| Restart persistence                   | 2     |
| Create class offline                  | 3     |
| Add attribute offline                 | 2     |
| Sync empty queue                      | 1     |
| Sequential replay (N→N+1→N+2)        | 2     |
| Rebase from stale baseVersion         | 1     |
| 409 stops replay                      | 2     |
| Failed domain command stays queued    | 1     |
| Successful commands removed           | 1     |
| Local ID mapping                      | 2     |
| Final cache refresh                   | 1     |
| STOMP version guard                   | 3     |
| Transport error detection             | 2     |
| offline_sync_test (regresión)         | 2     |
| uml_mobile_test (regresión)           | 4     |
| widget_test                           | 1     |
| **Total**                             | **40** |
