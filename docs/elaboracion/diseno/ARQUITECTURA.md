# Arquitectura del Sistema
**Versión:** 0.1 | **Fecha:** 2026-09-20 | **Fase UP:** Elaboración

---

## 1. Visión General

```
┌─────────────────────────────────────────────────────────────┐
│                    HERRAMIENTA CASE                         │
│                                                             │
│  Angular 17 (Editor UML - GoJS [PENDIENTE])                │
│           │                                                 │
│           │ HTTP REST + WebSocket STOMP                    │
│           ▼                                                 │
│  Spring Boot 3.x (Backend CASE) [DECISIÓN DE DISEÑO]      │
│    ┌──────┴──────────────────────┐                         │
│    │ domain/  application/       │                         │
│    │ infrastructure/             │                         │
│    └──────┬──────────────────────┘                         │
│           │ Puerto → Adaptador JPA                         │
│           ▼                                                 │
│  PostgreSQL (Docker Compose)                                │
└─────────────────────────┬───────────────────────────────────┘
                          │ Generador (Fase 6+)
                          ▼
          ┌──────────────────────────────────┐
          │  BACKEND GENERADO [DOCENTE]      │
          │  Spring Boot + PostgreSQL        │
          │  Entity/Repository/Service/Ctrl  │
          └──────────────────────────────────┘
```

---

## 2. Capas del Backend CASE

```
com.umlcase/
│
├── domain/               ← Reglas de negocio UML. Sin Spring, sin JPA.
│   ├── model/            ← UmlModel, UmlClass, UmlAttribute, UmlOperation,
│   │                        UmlRelationship, UmlDiagram, UmlNodeView
│   ├── command/          ← UmlCommand (sealed interface) y sus implementaciones
│   └── port/             ← Interfaces: UmlModelRepository, DiagramRepository
│
├── application/          ← Casos de uso. Orquesta dominio + puertos.
│   └── handler/          ← CommandHandler (procesa UmlCommand)
│
└── infrastructure/       ← Implementaciones concretas.
    ├── persistence/      ← Entidades JPA + implementaciones de puertos
    └── web/              ← Controllers REST + WebSocket handlers
```

---

## 3. Separación Semántica / Visual (ADR-002)

```
UmlModel (semántica)          UmlDiagram (visual)
────────────────────          ──────────────────────────
UmlClass                      UmlNodeView
  id ←───────────────────────── elementId
  name                          x, y, width, height
  attributes[]
  operations[]

UmlRelationship               UmlEdgeView
  id ←───────────────────────── relationshipId
  type                          (waypoints si necesario)
  sourceClassId
  targetClassId
  multiplicities
```

**Regla:** Mover un nodo en el canvas produce un `MoveNodeCommand` que solo actualiza `UmlNodeView`. No toca `UmlClass`.

---

## 4. Flujo de un Comando (Fase 0 — concepto)

```
Usuario en Angular
      │
      │ HTTP POST /api/projects/{id}/commands
      │ Body: UmlCommand (JSON)
      ▼
CommandController (infrastructure/web)
      │
      ▼
CommandHandler (application)
      │ valida + aplica al dominio
      ▼
UmlModel (domain) — modificado en memoria
      │
      │ mediante puerto UmlModelRepository
      ▼
UmlModelJpaRepository (infrastructure/persistence)
      │
      ▼
PostgreSQL

[Fase 3+: el CommandHandler también hace broadcast vía STOMP]
```

---

## 5. Stack Tecnológico

| Capa | Tecnología | Clasificación |
|---|---|---|
| Editor frontend | Angular 17 | `[DECISIÓN DE DISEÑO]` |
| Biblioteca de diagramación | GoJS | `[PENDIENTE / CANDIDATO PRINCIPAL]` |
| Backend CASE | Spring Boot 3.x, Java 17 | `[DECISIÓN DE DISEÑO]` |
| ORM | Spring Data JPA / Hibernate | `[DECISIÓN DE DISEÑO]` |
| Tiempo real | Spring WebSocket / STOMP | `[DECISIÓN DE DISEÑO]` |
| Base de datos CASE | PostgreSQL | `[DECISIÓN DE DISEÑO]` |
| Contenedores dev | Docker Compose | `[DECISIÓN DE DISEÑO]` |
| Auth | JWT (Fase 1+) | `[DECISIÓN DE DISEÑO]` |
| Backend generado | Spring Boot | `[DOCENTE]` |
| BD del generado | PostgreSQL | `[DOCENTE]` |
| Frontend móvil | Flutter | `[DOCENTE - muy fuerte]` |
| Nube | AWS | `[DOCENTE]` |
| Build tool | Maven Wrapper | `[DECISIÓN DE DISEÑO]` |

---

## 6. Decisiones Arquitectónicas

Ver `docs/elaboracion/adr/` para el detalle completo de cada ADR.

| ADR | Decisión |
|---|---|
| ADR-001 | Arquitectura de puertos y adaptadores para dominio UML |
| ADR-002 | Separación semántica UML / representación visual |
| ADR-003 | Spring Boot para backend de la CASE |
| ADR-004 | Command Pattern para modificaciones UML |
| ADR-005 | Control optimista de concurrencia con @Version |
