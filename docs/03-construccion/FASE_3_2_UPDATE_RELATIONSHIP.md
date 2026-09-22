# FASE 3.2: UPDATE RELATIONSHIP COLABORATIVO

## Objetivo
Implementar la actualización colaborativa de relaciones UML, asegurando la propagación en tiempo real sin requerir recargar la página y manteniendo un control estricto de concurrencia.

## Componentes Implementados

### 1. Dominio y Casos de Uso
- **`UpdateRelationshipCommand`**: Comando que encapsula la intención de actualización.
- **`UpdateRelationshipUseCase`**: Interfaz del puerto de entrada.
- **`UpdateRelationshipHandler`**: Manejador del caso de uso. Verifica la existencia de la relación y coordina la actualización transaccional en `UmlModel`.
- **`RelationshipUpdatedEvent`**: Evento de dominio emitido (`AFTER_COMMIT`) cuando la actualización es exitosa.

### 2. Capa Web (REST API)
- **Endpoint**: `PUT /api/projects/{projectId}/relationships/{relationshipId}`
- **Request DTO**: `UpdateRelationshipRequest`
- **Response DTO**: `UpdateRelationshipResponse`

#### Campos Permitidos para Edición (Editables)
- `type` (como `RelationshipType`: ASSOCIATION, GENERALIZATION, AGGREGATION, COMPOSITION)
- `sourceMultiplicity`
- `targetMultiplicity`

#### Campos Inmutables
- `relationshipId` (viene por URL y no se sobreescribe)
- `sourceClassId` (no modificable, garantiza integridad topológica)
- `targetClassId` (no modificable, garantiza integridad topológica)

### 3. Persistencia y Control de Concurrencia
- **Optimistic Locking**: Valida `expectedVersion` contra el `@Version` de JPA en la entidad del modelo.
- Incremento atómico y estricto `N → N+1` una sola vez por transacción.
- La respuesta HTTP devuelve la versión final (`response.modelVersion = savedModel.getVersion()`).

### 4. WebSocket (STOMP)
- Evento propagado: `RELATIONSHIP_UPDATED`.
- Despachado por `StompUmlEventListener` en fase `AFTER_COMMIT` para asegurar que el consumidor solo reciba versiones efectivamente guardadas.

## Evidencia Técnica y Pruebas

La implementación incluye cobertura estricta de pruebas y ha sido verificada tanto unitariamente como en integración y manualmente:

```text
Tests run: 110
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS

Angular build SUCCESS
```

### Evidencia de Demo Colaborativa (A/B)
```text
53 → 54
STOMP B real
F5 A/B permanece 54
COMPOSITION
1
1..*
```
- A edita la relación (por ejemplo: `Correo → ClienteVIP1`) con tipo `COMPOSITION` y multiplicidades `1` a `1..*`.
- Ambas ventanas (A y B) reciben la actualización STOMP al instante, quedando ambas en la versión 54.
- `GET /model` en F5 reconstruye correctamente el estado persistido. No se genera un salto a N+2, ni duplicación de la relación, preservándose `sourceClassId` y `targetClassId`.

## Notas de Diseño
- Update Relationship usa `PUT` (reemplazo/actualización del recurso) a diferencia del `POST` usado en la creación.
