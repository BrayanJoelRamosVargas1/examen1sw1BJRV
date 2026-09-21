# Matriz de Trazabilidad
**Versión:** 0.1 | **Fecha:** 2026-09-20 | **Fase UP:** Inicio

---

## Instrucciones de Uso

Esta matriz garantiza la regla del docente:
> *"Todo lo que está en la documentación debe estar en el software, y todo lo que está en el software debe estar en la documentación."*

**Estados de columnas:**
- `[—]` No aplica en esta fase
- `[PENDIENTE]` Aún no implementado
- `[FASE 0]` Implementado en Fase 0
- `[OK]` Verificado y funcionando

---

## Requisitos Funcionales

| ID | Requisito | Clasificación | Componente de Código | Prueba | Sección Docs | Defensa Oral |
|---|---|---|---|---|---|---|
| RF-01 | Crear diagramas de clases UML | `[DOCENTE]` | `UmlModel`, `UmlClass` (dominio puro) | `[PENDIENTE - F1]` | BASELINE §2, ADR-003 | `[PENDIENTE]` |
| RF-02 | Edición manual del diagrama | `[DOCENTE]` | `UmlCommand`, `CommandHandler` | `[PENDIENTE - F1]` | ADR-004 | `[PENDIENTE]` |
| RF-03 | Edición mediante comandos de voz | `[DOCENTE]` | `VoiceAdapter` (futura) | `[PENDIENTE - F8]` | — | `[PENDIENTE]` |
| RF-04 | Crear modelo desde fotografía | `[DOCENTE]` | `ImageImporter` (futuro) | `[PENDIENTE - F9]` | — | `[PENDIENTE]` |
| RF-05 | Trabajo colaborativo | `[DOCENTE]` | `ProjectRoom`, WebSocket | `[PENDIENTE - F3]` | — | `[PENDIENTE]` |
| RF-06 | Sincronización tiempo real | `[DOCENTE]` | STOMP broadcast | `[PENDIENTE - F3]` | — | `[PENDIENTE]` |
| RF-07 | Manejo de concurrencia | `[DOCENTE]` | `@Version` (optimista) + serialización futura | `[PENDIENTE - F3]` | ADR-005 | `[PENDIENTE]` |
| REQ-08 | Importar desde Enterprise Architect | `[DOCENTE]` | `XmiImporter` (futuro) | `[PENDIENTE - F5]` | — | `[PENDIENTE]` |
| REQ-09 | Exportar hacia Enterprise Architect | `[DOCENTE]` | `XmiExporter` (futuro) | `[PENDIENTE - F5]` | — | `[PENDIENTE]` |
| REQ-09.1 | XMI como formato | `[PENDIENTE / MECANISMO]` | `XmiService` (futuro) | `[PENDIENTE - F5]` | — | `[PENDIENTE]` |
| RF-10 | Notación UML 2.5+ | `[DOCENTE]` | Modelo canónico + GoJS | `[PENDIENTE - F2]` | BASELINE §3 | `[PENDIENTE]` |
| RF-11 | Generar backend Spring Boot | `[DOCENTE]` | `SpringBootGenerator` (futuro) | `[PENDIENTE - F6]` | — | `[PENDIENTE]` |
| RF-12 | Backend generado con PostgreSQL | `[DOCENTE]` | Plantillas + Docker Compose generado | `[PENDIENTE - F6]` | — | `[PENDIENTE]` |
| RF-13 | Transformación OO → relacional | `[DOCENTE]` | `OoToRelationalTransformer` (futuro) | `[PENDIENTE - F6]` | — | `[PENDIENTE]` |
| RF-14 | 4 capas Spring Boot en generado | `[DOCENTE]` | Plantillas Entity/Repo/Svc/Ctrl | `[PENDIENTE - F6]` | — | `[PENDIENTE]` |
| RF-15 | DTO opcional en generado | `[DOCENTE]` | Plantilla DTO | `[PENDIENTE - F6]` | — | `[PENDIENTE]` |
| RF-16 | Backend ejecutable (mvn run) | `[DOCENTE]` | CI de prueba del generado | `[PENDIENTE - F7]` | — | `[PENDIENTE]` |
| RF-17 | Probar backend generado | `[DOCENTE]` | Suite de pruebas automáticas | `[PENDIENTE - F7]` | — | `[PENDIENTE]` |
| RF-18 | IA integrada en producto | `[DOCENTE]` | Asistente + Imagen→UML | `[PENDIENTE - F10]` | — | `[PENDIENTE]` |
| RF-19 | Asistente de usuario | `[DOCENTE]` | `AssistantService` (futuro) | `[PENDIENTE - F10]` | — | `[PENDIENTE]` |
| RF-20 | Frontend móvil para la aplicación generada | `[DOCENTE]` | App móvil | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-20.1 | Desarrollo móvil con Flutter | `[PENDIENTE / CANDIDATO PRINCIPAL]` | App Flutter | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| REQ-22 | Frontend móvil de la CASE | `[DOCENTE]` | App móvil | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-21 | App móvil offline | `[DOCENTE]` | Cola offline + sync | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-22 | IA local en móvil | `[DOCENTE]` | Modelo on-device | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-23 | Sincronización al reconectar | `[DOCENTE]` | Sync service | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-24 | Despliegue AWS | `[DOCENTE]` | IaC / Docker en EC2/ECS | `[PENDIENTE - F12]` | — | `[PENDIENTE]` |

---

## Componentes de Infraestructura Fase 0

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| INF-01 | Repositorio Git independiente | `[FASE 0]` | `git status` | `[OK]` |
| INF-02 | Estructura documental UP | `[FASE 0]` | Revisión manual | `[OK]` |
| INF-03 | `REQUISITOS_DOCENTE.md` | `[FASE 0]` | Revisión manual | Este documento / `docs/00-baseline` |
| INF-04 | `GLOSARIO.md` | `[FASE 0]` | Revisión manual | `docs/00-baseline/GLOSARIO.md` |
| INF-05 | ADR creados | `[FASE 0]` | Revisión manual | `docs/02-elaboracion/adr/` |
| INF-06 | Modelo dominio UML puro (Java) | `[FASE 0]` | Unit test dominio | `[OK]` |
| INF-07 | Modelo visual separado | `[FASE 0]` | Revisión manual | `[OK]` |
| INF-08 | Contrato `UmlCommand` | `[FASE 0]` | Revisión manual | `[OK]` |
| INF-09 | Puerto `UmlModelRepository` | `[FASE 0]` | Revisión manual | `[OK]` |
| INF-10 | PostgreSQL vía Docker Compose | `[FASE 0]` | `docker compose ps` | `[OK]` |
| INF-11 | Spring Boot mínimo + `/api/health` | `[FASE 0]` | `curl` / test HTTP | `[OK]` |
| INF-12 | Angular mínimo compilando | `[FASE 0]` | `npm run build` | `[OK]` |

---

## Componentes de Fase 1 (Slice vertical: CREATE_CLASS)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F1-01 | GET `/api/projects/{projectId}/model` | `[FASE 1]` | `UmlProjectControllerTest` | `[OK]` |
| F1-02 | POST `/api/projects/{projectId}/classes` | `[FASE 1]` | `CreateClassApiIT` | `[OK]` |
| F1-03 | Persistencia PostgreSQL + Flyway | `[FASE 1]` | `JpaUmlModelRepositoryAdapterIT` | `[OK]` |
| F1-04 | Concurrencia optimista (`@Version`) | `[FASE 1]` | `CreateClassTransactionalIT` | `[OK]` |
| F1-05 | Publicación interna `AFTER_COMMIT` | `[FASE 1]` | `CreateClassTransactionalIT` | `[OK]` |
| F1-06 | Broadcast STOMP a clientes | `[FASE 1]` | Demo manual A/B | `[OK]` |
| F1-07 | Frontend Angular reactivo | `[FASE 1]` | Demo manual A/B | `[OK]` |
| F1-08 | Persistencia comprobable tras F5 | `[FASE 1]` | Demo manual F5 | `[OK]` |

---

## Componentes de Fase 2.1 (Rename)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.1-01 | PATCH `/api/projects/{projectId}/classes/{classId}/rename` | `[FASE 2.1]` | `RenameClassApiIT` | `[OK]` |
| F2.1-02 | STOMP broadcast CLASS_RENAMED | `[FASE 2.1]` | Demo A/B | `[OK]` |
| F2.1-03 | Frontend / Angular actualización | `[FASE 2.1]` | Demo A/B | `[OK]` |
| F2.1-04 | 409 Conflict desacoplado | `[FASE 2.1]` | `RenameClassHandlerTest` | `[OK]` |

---

## Componentes de Fase 2.2.1 (Add Attribute)

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| F2.2.1-01 | POST `/api/projects/{projectId}/classes/{classId}/attributes` | `[FASE 2.2.1]` | `AddAttributeApiIT` | `[OK]` |
| F2.2.1-02 | Persistencia V3 (uml_attributes) y orderIndex | `[FASE 2.2.1]` | `AddAttributeTransactionalIT` | `[OK]` |
| F2.2.1-03 | STOMP broadcast ATTRIBUTE_ADDED | `[FASE 2.2.1]` | Demo A/B | `[OK]` |
| F2.2.1-04 | Frontend visualiza atributos | `[FASE 2.2.1]` | Demo A/B y F5 | `[OK]` |
