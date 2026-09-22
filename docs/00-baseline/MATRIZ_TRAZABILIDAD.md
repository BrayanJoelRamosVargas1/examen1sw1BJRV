# Matriz de Trazabilidad
**VersiÃ³n:** 0.1 | **Fecha:** 2026-09-20 | **Fase UP:** Inicio

---

## Instrucciones de Uso

Esta matriz garantiza la regla del docente:
> *"Todo lo que estÃ¡ en la documentaciÃ³n debe estar en el software, y todo lo que estÃ¡ en el software debe estar en la documentaciÃ³n."*

**Estados de columnas:**
- `[â€”]` No aplica en esta fase
- `[PENDIENTE]` AÃºn no implementado
- `[FASE 0]` Implementado en Fase 0
- `[OK]` Verificado y funcionando

---

## Requisitos Funcionales

| ID | Requisito | ClasificaciÃ³n | Componente de CÃ³digo | Prueba | SecciÃ³n Docs | Defensa Oral |
|---|---|---|---|---|---|---|
| RF-01 | Crear diagramas de clases UML | `[DOCENTE]` | `UmlModel`, `UmlClass` (dominio puro) | `[PENDIENTE - F1]` | BASELINE Â§2, ADR-003 | `[PENDIENTE]` |
| RF-02 | EdiciÃ³n manual del diagrama | `[DOCENTE]` | `UmlCommand`, `CommandHandler` | `[PENDIENTE - F1]` | ADR-004 | `[PENDIENTE]` |
| RF-03 | EdiciÃ³n mediante comandos de voz | `[DOCENTE]` | `VoiceAdapter` (futura) | `[PENDIENTE - F8]` | â€” | `[PENDIENTE]` |
| RF-04 | Crear modelo desde fotografÃ­a | `[DOCENTE]` | `ImageImporter` (futuro) | `[PENDIENTE - F9]` | â€” | `[PENDIENTE]` |
| RF-05 | Trabajo colaborativo | `[DOCENTE]` | `ProjectRoom`, WebSocket | `[PENDIENTE - F3]` | â€” | `[PENDIENTE]` |
| RF-06 | SincronizaciÃ³n tiempo real | `[DOCENTE]` | STOMP broadcast | `[PENDIENTE - F3]` | â€” | `[PENDIENTE]` |
| RF-07 | Manejo de concurrencia | `[DOCENTE]` | `@Version` (optimista) + serializaciÃ³n futura | `[PENDIENTE - F3]` | ADR-005 | `[PENDIENTE]` |
| REQ-08 | Importar desde Enterprise Architect | `[DOCENTE]` | `XmiImporter` (futuro) | `[PENDIENTE - F5]` | â€” | `[PENDIENTE]` |
| REQ-09 | Exportar hacia Enterprise Architect | `[DOCENTE]` | `XmiExporter` (futuro) | `[PENDIENTE - F5]` | â€” | `[PENDIENTE]` |
| REQ-09.1 | XMI como formato | `[PENDIENTE / MECANISMO]` | `XmiService` (futuro) | `[PENDIENTE - F5]` | â€” | `[PENDIENTE]` |
| RF-10 | NotaciÃ³n UML 2.5+ | `[DOCENTE]` | Modelo canÃ³nico + GoJS | `[PENDIENTE - F2]` | BASELINE Â§3 | `[PENDIENTE]` |
| RF-11 | Generar backend Spring Boot | `[DOCENTE]` | `SpringBootGenerator` (futuro) | `[PENDIENTE - F6]` | â€” | `[PENDIENTE]` |
| RF-12 | Backend generado con PostgreSQL | `[DOCENTE]` | Plantillas + Docker Compose generado | `[PENDIENTE - F6]` | â€” | `[PENDIENTE]` |
| RF-13 | TransformaciÃ³n OO â†’ relacional | `[DOCENTE]` | `OoToRelationalTransformer` (futuro) | `[PENDIENTE - F6]` | â€” | `[PENDIENTE]` |
| RF-14 | 4 capas Spring Boot en generado | `[DOCENTE]` | Plantillas Entity/Repo/Svc/Ctrl | `[PENDIENTE - F6]` | â€” | `[PENDIENTE]` |
| RF-15 | DTO opcional en generado | `[DOCENTE]` | Plantilla DTO | `[PENDIENTE - F6]` | â€” | `[PENDIENTE]` |
| RF-16 | Backend ejecutable (mvn run) | `[DOCENTE]` | CI de prueba del generado | `[PENDIENTE - F7]` | â€” | `[PENDIENTE]` |
| RF-17 | Probar backend generado | `[DOCENTE]` | Suite de pruebas automÃ¡ticas | `[PENDIENTE - F7]` | â€” | `[PENDIENTE]` |
| RF-18 | IA integrada en producto | `[DOCENTE]` | Asistente + Imagenâ†’UML | `[PENDIENTE - F10]` | â€” | `[PENDIENTE]` |
| RF-19 | Asistente de usuario | `[DOCENTE]` | `AssistantService` (futuro) | `[PENDIENTE - F10]` | â€” | `[PENDIENTE]` |
| RF-20 | Frontend mÃ³vil para la aplicaciÃ³n generada | `[DOCENTE]` | App mÃ³vil | `[PENDIENTE - F11]` | â€” | `[PENDIENTE]` |
| RF-20.1 | Desarrollo mÃ³vil con Flutter | `[PENDIENTE / CANDIDATO PRINCIPAL]` | App Flutter | `[PENDIENTE - F11]` | â€” | `[PENDIENTE]` |
| REQ-22 | Frontend mÃ³vil de la CASE | `[DOCENTE]` | App mÃ³vil | `[PENDIENTE - F11]` | â€” | `[PENDIENTE]` |
| RF-21 | App mÃ³vil offline | `[DOCENTE]` | Cola offline + sync | `[PENDIENTE - F11]` | â€” | `[PENDIENTE]` |
| RF-22 | IA local en mÃ³vil | `[DOCENTE]` | Modelo on-device | `[PENDIENTE - F11]` | â€” | `[PENDIENTE]` |
| RF-23 | SincronizaciÃ³n al reconectar | `[DOCENTE]` | Sync service | `[PENDIENTE - F11]` | â€” | `[PENDIENTE]` |
| RF-24 | Despliegue AWS | `[DOCENTE]` | IaC / Docker en EC2/ECS | `[PENDIENTE - F12]` | â€” | `[PENDIENTE]` |

---

## Componentes de Infraestructura Fase 0

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| INF-01 | Repositorio Git independiente | `[FASE 0]` | `git status` | `[OK]` |
| INF-02 | Estructura documental UP | `[FASE 0]` | RevisiÃ³n manual | `[OK]` |
| INF-03 | `REQUISITOS_DOCENTE.md` | `[FASE 0]` | RevisiÃ³n manual | Este documento / `docs/00-baseline` |
| INF-04 | `GLOSARIO.md` | `[FASE 0]` | RevisiÃ³n manual | `docs/00-baseline/GLOSARIO.md` |
| INF-05 | ADR creados | `[FASE 0]` | RevisiÃ³n manual | `docs/02-elaboracion/adr/` |
| INF-06 | Modelo dominio UML puro (Java) | `[FASE 0]` | Unit test dominio | `[OK]` |
| INF-07 | Modelo visual separado | `[FASE 0]` | RevisiÃ³n manual | `[OK]` |
| INF-08 | Contrato `UmlCommand` | `[FASE 0]` | RevisiÃ³n manual | `[OK]` |
| INF-09 | Puerto `UmlModelRepository` | `[FASE 0]` | RevisiÃ³n manual | `[OK]` |
| INF-10 | PostgreSQL vÃ­a Docker Compose | `[FASE 0]` | `docker compose ps` | `[OK]` |
| INF-11 | Spring Boot mÃ­nimo + `/api/health` | `[FASE 0]` | `curl` / test HTTP | `[OK]` |
| INF-12 | Angular mÃ­nimo compilando | `[FASE 0]` | `npm run build` | `[OK]` |

---

## Componentes de Fase 1 (Slice vertical: CREATE_CLASS)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F1-01 | GET `/api/projects/{projectId}/model` | `[FASE 1]` | `UmlProjectControllerTest` | `[OK]` |
| F1-02 | POST `/api/projects/{projectId}/classes` | `[FASE 1]` | `CreateClassApiIT` | `[OK]` |
| F1-03 | Persistencia PostgreSQL + Flyway | `[FASE 1]` | `JpaUmlModelRepositoryAdapterIT` | `[OK]` |
| F1-04 | Concurrencia optimista (`@Version`) | `[FASE 1]` | `CreateClassTransactionalIT` | `[OK]` |
| F1-05 | PublicaciÃ³n interna `AFTER_COMMIT` | `[FASE 1]` | `CreateClassTransactionalIT` | `[OK]` |
| F1-06 | Broadcast STOMP a clientes | `[FASE 1]` | Demo manual A/B | `[OK]` |
| F1-07 | Frontend Angular reactivo | `[FASE 1]` | Demo manual A/B | `[OK]` |
| F1-08 | Persistencia comprobable tras F5 | `[FASE 1]` | Demo manual F5 | `[OK]` |

---

## Componentes de Fase 2.1 (Rename)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.1-01 | PATCH `/api/projects/{projectId}/classes/{classId}/rename` | `[FASE 2.1]` | `RenameClassApiIT` | `[OK]` |
| F2.1-02 | STOMP broadcast CLASS_RENAMED | `[FASE 2.1]` | Demo A/B | `[OK]` |
| F2.1-03 | Frontend / Angular actualizaciÃ³n | `[FASE 2.1]` | Demo A/B | `[OK]` |
| F2.1-04 | 409 Conflict desacoplado | `[FASE 2.1]` | `RenameClassHandlerTest` | `[OK]` |

---

## Componentes de Fase 2.2.1 (Add Attribute)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.2.1-01 | POST `/api/projects/{projectId}/classes/{classId}/attributes` | `[FASE 2.2.1]` | `AddAttributeApiIT` | `[OK]` |
| F2.2.1-02 | Persistencia V3 (uml_attributes) y orderIndex | `[FASE 2.2.1]` | `AddAttributeTransactionalIT` | `[OK]` |
| F2.2.1-03 | STOMP broadcast ATTRIBUTE_ADDED | `[FASE 2.2.1]` | Demo A/B | `[OK]` |
| F2.2.1-04 | Frontend visualiza atributos | `[FASE 2.2.1]` | Demo A/B y F5 | `[OK]` |

---

## Componentes de Fase 2.2.2 (Update Attribute)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.2.2-01 | PUT `/api/projects/{projectId}/classes/{classId}/attributes/{attributeId}` | `[FASE 2.2.2]` | `UpdateAttributeApiIT` | `[OK]` |
| F2.2.2-02 | STOMP broadcast ATTRIBUTE_UPDATED | `[FASE 2.2.2]` | Demo A/B | `[OK]` |
| F2.2.2-03 | DeduplicaciÃ³n STOMP para el autor del HTTP 200 | `[FASE 2.2.2]` | Demo A/B | `[OK]` |
| F2.2.2-04 | SeparaciÃ³n lÃ³gica Frontend Add vs Update | `[FASE 2.2.2]` | Demo A/B | `[OK]` |
| F2.2.2-05 | 409 Conflict Optimistic Locking | `[FASE 2.2.2]` | `UpdateAttributeApiIT` | `[OK]` |

---

## Componentes de Fase 2.2.3 (Remove Attribute)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.2.3-01 | DELETE `/api/projects/{projectId}/classes/{classId}/attributes/{attributeId}` | `[FASE 2.2.3]` | `RemoveAttributeApiIT` | `[OK]` |
| F2.2.3-02 | STOMP broadcast ATTRIBUTE_REMOVED | `[FASE 2.2.3]` | Demo A/B | `[OK]` |
| F2.2.3-03 | EstabilizaciÃ³n Race Condition STOMP/HTTP/GET | `[FASE 2.2.3]` | Demo A/B y F5 | `[OK]` |

---

## Componentes de Fase 2.3.1 (Add Operation + Parameters)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.3.1-01 | POST `/api/projects/{projectId}/classes/{classId}/operations` | `[FASE 2.3.1]` | `AddOperationApiIT` | `[OK]` |
| F2.3.1-02 | Persistencia V4 (uml_operations, uml_parameters) y orden | `[FASE 2.3.1]` | `AddOperationTransactionalIT` | `[OK]` |
| F2.3.1-03 | Refactor JPA a `Set<LinkedHashSet>` anti MultipleBagFetchException | `[FASE 2.3.1]` | Suite 75 tests completa | `[OK]` |
| F2.3.1-04 | STOMP broadcast OPERATION_ADDED con parÃ¡metros | `[FASE 2.3.1]` | Demo A/B | `[OK]` |
| F2.3.1-05 | PolÃ­tica de Sobrecarga (Overloading) de Firmas UML | `[FASE 2.3.1]` | `UmlClassTest` | `[OK]` |
| F2.3.1-06 | Frontend notaciÃ³n UML (`+ nombre(p: Tipo): Ret`) | `[FASE 2.3.1]` | Demo A/B y F5 | `[OK]` |
---

## Componentes de Fase 2.3.2 (Update Operation + Parameters)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.3.2-01 | PUT /api/projects/{projectId}/classes/{classId}/operations/{operationId} | [FASE 2.3.2] | UpdateOperationApiIT | [OK] |
| F2.3.2-02 | Bidireccionalidad Operation->Parameter para dirty-checking estable | [FASE 2.3.2] | UpdateOperationTransactionalIT | [OK] |
| F2.3.2-03 | STOMP broadcast OPERATION_UPDATED + Fix STOMP test | [FASE 2.3.2] | UpdateOperationStompIT | [OK] |
| F2.3.2-04 | Fix markModified monotónico forzado para @Version | [FASE 2.3.2] | UpdateOperationTransactionalIT | [OK] |
| F2.3.2-05 | Fix Caché GET /model en navegadores
o-cache | [FASE 2.3.2] | Demo F5 A/B | [OK] |
---

## Componentes de Fase 2.3.3 (Remove Operation)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.3.3-01 | DELETE /api/projects/{projectId}/classes/{classId}/operations/{operationId} | [FASE 2.3.3] | RemoveOperationApiIT | [OK] |
| F2.3.3-02 | OperationRemovedEvent + STOMP broadcast | [FASE 2.3.3] | RemoveOperationStompIT | [OK] |
| F2.3.3-03 | Frontend tipado de eventos sin casts unsafe | [FASE 2.3.3] | Angular build | [OK] |
| F2.3.3-04 | Optimistic Locking @Version N+1 y orphan removal en cascade | [FASE 2.3.3] | RemoveOperationTransactionalIT | [OK] |

---

## Componentes de Fase 2.4 (NodeView / Persistencia Layout)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.4-01 | PUT /api/projects/{projectId}/diagram/nodes | [FASE 2.4] | SaveNodeViewApiIT | [OK] |
| F2.4-02 | GET /api/projects/{projectId}/diagram (read-only) | [FASE 2.4] | SaveNodeViewApiIT | [OK] |
| F2.4-03 | Entidad separada JpaUmlDiagramLayoutEntity | [FASE 2.4] | DB Migration V5 | [OK] |
| F2.4-04 | Optimistic Locking @Version (409 Conflict) en Layout | [FASE 2.4] | UmlDiagramLayoutRepositoryAdapterIT | [OK] |
| F2.4-05 | Invarianza: modelVersion inmutable en mutacin visual | [FASE 2.4] | SaveNodeViewTransactionalIT | [OK] |

---

## Componentes de Fase 2.5 (Move Node + STOMP Layout Sync)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.5-01 | PUT /api/projects/{projectId}/diagram/nodes/{classId} on drag end | [FASE 2.5] | Demo manual A/B | [OK] |
| F2.5-02 | STOMP broadcast NODE_MOVED en /topic/projects/{projectId}/diagram | [FASE 2.5] | NodeMovedStompIT | [OK] |
| F2.5-03 | Frontend Pointer Events + Posicionamiento Absoluto | [FASE 2.5] | Angular build | [OK] |

---

## Componentes de Fase 2.6 (Demo Integral Fase 2)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.6-01 | Auditoría de integridad arquitectónica (Model vs Layout) | [FASE 2.6] | Demo A/B Integral | [OK] |
| F2.6-02 | Suite completada sin regresiones (100 tests) | [FASE 2.6] | mvnw test | [OK] |
| F2.6-03 | Auditoría de CSS y Build estático | [FASE 2.6] | 
pm run build | [OK] |

---

**FASE 2 COMPLETADA SATISFACTORIAMENTE.**

---

## Componentes de Fase 3.0 (Arquitectura Relationships)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F3.0-01 | UmlRelationship de dominio (pureza, tipos) | `[FASE 3.0]` | ADR/Docs | `[OK]` |

---

## Componentes de Fase 3.1 (Add Relationship)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F3.1-01 | POST `/api/projects/{projectId}/relationships` | `[FASE 3.1]` | `AddRelationshipApiIT` | `[OK]` |
| F3.1-02 | Persistencia V6 (uml_relationships) + Optimistic Locking | `[FASE 3.1]` | `AddRelationshipTransactionalIT` | `[OK]` |
| F3.1-03 | STOMP broadcast RELATIONSHIP_ADDED | `[FASE 3.1]` | Node script / Demo | `[OK]` |
| F3.1-04 | GET /model serializa relationships | `[FASE 3.1]` | `AddRelationshipApiIT` | `[OK]` |
| F3.1-05 | Fix bug STOMP propagation | `[FASE 3.1]` | `StompUmlEventListenerTest` | `[OK]` |

---

## Componentes de Fase 3.2 (Update Relationship)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F3.2-01 | PUT `/api/projects/{projectId}/relationships/{relationshipId}` | `[FASE 3.2]` | `UpdateRelationshipApiIT` | `[OK]` |
| F3.2-02 | STOMP broadcast RELATIONSHIP_UPDATED | `[FASE 3.2]` | Demo A/B STOMP real | `[OK]` |
| F3.2-03 | Inmutabilidad de endpoints/extremos topológicos | `[FASE 3.2]` | `UpdateRelationshipTransactionalIT` | `[OK]` |
| F3.2-04 | Update muta sólo metadata (tipo y multiplicidades) | `[FASE 3.2]` | `UpdateRelationshipHandlerTest` | `[OK]` |
