# FASE_1_CREATE_CLASS

## Caso/alcance:
- CREATE_CLASS sobre proyecto UML existente

## Escritura:
- POST /api/projects/{projectId}/classes

## Lectura:
- GET /api/projects/{projectId}/model

## Persistencia:
- PostgreSQL + Flyway

## Concurrencia:
- expectedVersion + JPA @Version
> [!NOTE]
> [DECISIÓN DE DISEÑO TEMPORAL — FASE 1]
> El optimistic locking resuelve la concurrencia a nivel de DB evitando que actualizaciones simultáneas pisen datos entre sí para esta prueba inicial.

## Evento:
- publicación interna
- → COMMIT
- → AFTER_COMMIT
- → STOMP (usando el broker simple de Spring)

## Frontend:
- Angular mínimo
- Angular usa sockjs-client y @stomp/stompjs
> [!NOTE]
> [DECISIÓN DE DISEÑO TEMPORAL — FASE 1]
> El UUID de proyecto/cliente es fijo para facilitar la demo manual rápida.

## Evidencia probada:
- 30 tests verdes
- Testcontainers PostgreSQL
- API integration
- commit → 1 evento
- rollback → 0 eventos
- A/B sincronizados sin F5
- F5 recupera ClienteFase1

> [!NOTE]
> [DOCENTE]
> El puerto de host default es 5433 por conflicto local, parametrizado mediante variable de entorno `UMLCASE_DB_PORT`.
