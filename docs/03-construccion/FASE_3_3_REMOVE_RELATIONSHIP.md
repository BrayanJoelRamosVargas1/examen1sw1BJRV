# FASE 3.3 - REMOVE RELATIONSHIP

## Objetivo
Implementar la eliminación de relaciones UML de forma colaborativa usando concurrencia optimista, propagando los eventos STOMP a todos los clientes.

## Componentes y Arquitectura

- **Comando:** `RemoveRelationshipCommand` (contiene `commandId`, `participantId`, `expectedVersion`, `relationshipId`)
- **Puerto de entrada:** `RemoveRelationshipUseCase`
- **Manejador:** `RemoveRelationshipHandler`
- **Evento:** `RelationshipRemovedEvent`
- **Endpoint API:** `DELETE /api/projects/{projectId}/relationships/{relationshipId}`

### Request / Metadata

Al eliminar, la petición incluye los siguientes parámetros de concurrencia:
- `commandId` (cabecera o query param para STOMP filter)
- `participantId` (identificador de la ventana emisora)
- `expectedVersion` (versión actual del modelo en el cliente)

### Response

El controlador responde con un JSON con el resultado o se envía por STOMP:
- `commandId`
- `relationshipId`
- `modelVersion` (la nueva versión del modelo)

### Concurrencia Optimista

Se utiliza la concurrencia optimista nativa de JPA (`@Version` en `JpaUmlModelEntity`). 
En el Handler:
- `expectedVersion` + `@Version`
- Si la versión esperada no coincide con la versión real del modelo antes de guardar, se lanza una excepción de conflicto (`ModelVersionConflictException`).
- `modelVersion` real se extrae después del save: `savedModel.getVersion()`.

### Bug Resuelto
Durante el desarrollo, inicialmente el `RemoveRelationshipHandler` incrementaba manualmente el `model.getVersion()` ejecutando `model.setVersion(N+1)`. Esto generaba una falla en el *Smart Merge* del adaptador JPA (`JpaUmlModelRepositoryAdapter`), arrojando prematuramente un error de conflicto de concurrencia.
**Solución:** Se corrigió eliminando `model.setVersion(N+1)`, dejando la responsabilidad de la concurrencia y el incremento del `@Version` exclusivamente a Hibernate (Optimistic Locking).

## Comportamiento del Sistema

- **AFTER_COMMIT:** El publicador de eventos propaga el mensaje `RELATIONSHIP_REMOVED` a través de STOMP solo después de que la transacción a la base de datos se confirma exitosamente.
- **Persistencia:** 
  - Fila de la tabla `uml_relationships` es eliminada físicamente (o desasociada del modelo).
  - Las clases de origen y destino (**source y target**) **no son eliminadas**, permanecen intactas.
- **GET /model:** 
  - Al hacer una nueva recarga (F5) para recuperar el estado inicial del modelo, la relación eliminada ya no aparece en el JSON resultante, y la versión refleja el incremento correcto.

## Evidencia de Demo

La prueba manual en navegador con dos clientes (A y B) demostró:
- Las versiones pasaron de `54 → 55`.
- Cliente B recibió `RELATIONSHIP_REMOVED v55` instantáneamente por STOMP sin necesidad de refrescar la página.
- Tras hacer `F5` en las pestañas A y B, la versión se mantiene en `55`.
- La relación `Tierras --[ASSOCIATION]--> auto` sigue ausente. Las otras relaciones de prueba se mantuvieron intactas y no hubo apariciones fantasma ni incrementos anómalos (`N+2`).
