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
| RF-08 | Importar desde EA (XMI) | `[DOCENTE]` | `XmiImporter` (futuro) | `[PENDIENTE - F5]` | — | `[PENDIENTE]` |
| RF-09 | Exportar hacia EA (XMI) | `[DOCENTE]` | `XmiExporter` (futuro) | `[PENDIENTE - F5]` | — | `[PENDIENTE]` |
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
| RF-20 | Frontend móvil (Flutter) | `[DOCENTE]` | App Flutter | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-21 | App móvil offline | `[DOCENTE]` | Cola offline + sync | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-22 | IA local en móvil | `[DOCENTE]` | Modelo on-device | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-23 | Sincronización al reconectar | `[DOCENTE]` | Sync service | `[PENDIENTE - F11]` | — | `[PENDIENTE]` |
| RF-24 | Despliegue AWS | `[DOCENTE]` | IaC / Docker en EC2/ECS | `[PENDIENTE - F12]` | — | `[PENDIENTE]` |

---

## Componentes de Infraestructura Fase 0

| ID | Componente | Estado | Prueba | Evidencia |
|---|---|---|---|---|
| INF-01 | Repositorio Git independiente | `[FASE 0]` | `git status` | `[PENDIENTE]` |
| INF-02 | Estructura documental UP | `[FASE 0]` | Revisión manual | `[PENDIENTE]` |
| INF-03 | `BASELINE_REQUISITOS.md` | `[FASE 0]` | Revisión manual | Este documento |
| INF-04 | `GLOSARIO.md` | `[FASE 0]` | Revisión manual | `docs/inicio/requisitos/GLOSARIO.md` |
| INF-05 | ADR creados | `[FASE 0]` | Revisión manual | `docs/elaboracion/adr/` |
| INF-06 | Modelo dominio UML puro (Java) | `[FASE 0]` | Unit test dominio | `[PENDIENTE]` |
| INF-07 | Modelo visual separado | `[FASE 0]` | Revisión manual | `[PENDIENTE]` |
| INF-08 | Contrato `UmlCommand` | `[FASE 0]` | Unit test | `[PENDIENTE]` |
| INF-09 | Puerto `UmlModelRepository` | `[FASE 0]` | Unit test con mock | `[PENDIENTE]` |
| INF-10 | PostgreSQL vía Docker Compose | `[PENDIENTE]` | `docker compose ps` | `[PENDIENTE]` |
| INF-11 | Spring Boot mínimo + `/api/health` | `[PENDIENTE]` | `curl` / test HTTP | `[PENDIENTE]` |
| INF-12 | Angular mínimo compilando | `[PENDIENTE]` | `npm run build` | `[PENDIENTE]` |
