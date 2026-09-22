# Fase 3.1: Add Relationship

Esta fase implementa la adición colaborativa de relaciones UML, cerrando el ciclo completo de diseño de arquitectura base (3.0) y materializando la persistencia, mutación concurrente (Optimistic Locking) y propagación STOMP.

## Arquitectura Implementada

Se implementó el ciclo de dominio y aplicación de acuerdo con la Fase 3.0:

*   **Modelo de Dominio**: `UmlRelationship`
*   **Enumeración**: `RelationshipType` con los siguientes valores:
    *   `ASSOCIATION`
    *   `GENERALIZATION`
    *   `AGGREGATION`
    *   `COMPOSITION`
*   **Capa de Aplicación**:
    *   `AddRelationshipCommand` (Uso de `UmlCommand.AddRelationship`)
    *   `AddRelationshipHandler` (Manejador de la transacción y lógica de negocio)
    *   `AddRelationshipUseCase` (Puerto de entrada)
    *   `RelationshipAddedEvent` (Evento de dominio publicado en el bus)

## Persistencia

*   Entidad JPA: `JpaUmlRelationshipEntity`
*   Evolución del Esquema: Se incluyó `V6__add_uml_relationships.sql`.
*   Obtención en lectura: `GET /model` ahora incluye `relationships[]` reconstruidos desde PostgreSQL.

## Concurrencia y Eventos (Optimistic Locking)

*   **Optimistic Locking**: Validación de `expectedVersion` vs `@Version` (`model.getVersion()`).
*   La mutación avanza atómicamente de la versión `N` a `N+1`.
*   El manejador publica el evento a través del `ApplicationEventPublisher`.
*   **Propagación STOMP**: `StompUmlEventListener` escucha el evento en la fase `AFTER_COMMIT` y lo propaga mediante `SimpMessagingTemplate` al topic `/topic/projects/{projectId}/events` (Manejado como tipo `RELATIONSHIP_ADDED`).

## Endpoint REST

*   Ruta: `POST /api/projects/{projectId}/relationships`

## Evidencia y Gate STOMP

La fase fue probada manualmente usando un script Node (`@stomp/stompjs` y `sockjs-client`). Los resultados en entorno local demostraron la efectividad asíncrona:

```text
demo real 51 → 52
B conectado realmente por STOMP
B recibió RELATIONSHIP_ADDED v52
relationshipId del evento == relationshipId REST
GET /model posterior == v52
relación persistida
```

### Bug Reportado y Corregido (STOMP Listener)

Durante el gate de integración STOMP real, se descubrió el siguiente defecto que eludía las pruebas iniciales de base de datos y REST puro:
*   Inicialmente faltaba el forwarding de `RelationshipAddedEvent` en `StompUmlEventListener`. El evento se emitía en el bus local de Spring pero no se reenviaba al broker WebSocket.
*   Se detectó mediante prueba STOMP real de la fase 3.1 y se corrigió de inmediato añadiendo el correspondiente `@TransactionalEventListener`. Adicionalmente, se construyó el test de regresión `StompUmlEventListenerTest` para bloquear reincidencias en esta vía de transmisión.
