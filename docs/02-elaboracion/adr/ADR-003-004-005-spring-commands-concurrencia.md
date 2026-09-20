# ADR-003: Spring Boot como Backend de la Herramienta CASE

**Fecha:** 2026-09-20
**Estado:** ACEPTADO
**Fase UP:** Elaboración

---

## Contexto

Los repositorios de referencia (`back-sw1`) usan **NestJS + TypeScript** como backend de la propia herramienta CASE. El docente exige `[DOCENTE]` que el **backend generado** por la herramienta sea Spring Boot. La pregunta es: ¿qué tecnología usamos para el backend **de la propia herramienta CASE**?

## Decisión

Usamos **Spring Boot** para ambos:

| Backend | Tecnología | Clasificación |
|---|---|---|
| Backend de la herramienta CASE | Spring Boot | `[DECISIÓN DE DISEÑO]` |
| Backend generado por la herramienta | Spring Boot | `[DOCENTE]` |

## Alternativas Consideradas

| Alternativa | Razón de rechazo |
|---|---|
| NestJS para CASE + Spring Boot para generado | Dos tecnologías a dominar y defender oralmente; aumenta el riesgo en la puerta 4 del examen |
| Spring Boot + NestJS híbrido | Innecesariamente complejo para el alcance del parcial |

## Consecuencias

- **Positivo:** Una sola tecnología de backend. El conocimiento de Spring Boot sirve tanto para explicar la CASE como el generado.
- **Positivo:** Mismos conceptos (Entity, Repository, Service, Controller) aparecen en ambos contextos, reforzando la coherencia ante el docente.
- **Negativo:** NestJS ya tiene implementada la sala colaborativa en el repo de referencia. Deberemos reimplementar WebSocket con Spring STOMP.
- **Decisión complementaria:** Usamos Spring WebSocket con STOMP en lugar de Socket.IO.

## Clasificación

`[DECISIÓN DE DISEÑO]` — El docente no especificó el backend de la CASE, solo el generado.

---

# ADR-004: Modelo de Comandos (Command Pattern) para Modificaciones UML

**Fecha:** 2026-09-20
**Estado:** ACEPTADO
**Fase UP:** Elaboración

---

## Contexto

La herramienta CASE debe soportar múltiples formas de entrada (manual, voz, imagen, XMI) que todas producen el mismo resultado: modificaciones al modelo UML. Adicionalmente, el docente puede pedir durante el examen que expliquemos y modifiquemos cualquier parte del código.

## Decisión

Adoptamos un **Command Pattern** para toda modificación al `UmlModel`.

Los comandos viven en **`application/command`**, **NO en el dominio**:

```
EXTERNO (API / voz / imagen / XMI)
            │
     UmlCommand  ← com.umlcase.application.command
            │
     CommandHandler ← com.umlcase.application.handler
            │
     UmlModel.addClass() / etc. ← com.umlcase.domain.model
```

El dominio UML (UmlModel, UmlClass, etc.) **no sabe que existen comandos**.
Los comandos son DTOs de intención de la capa de aplicación.

[CORRECCIÓN — Auditoría Fase 0] La ubicación inicial `domain/command` fue incorrecta. Corregida a `application/command`. El dominio no debe conocer la forma en que el exterior lo invoca.

Estructura del comando:

```java
// package com.umlcase.application.command  ← CORRECTO
// NO: com.umlcase.domain.command           ← INCORRECTO (corregido)
public sealed interface UmlCommand permits
    CreateClassCommand,
    RenameClassCommand,
    AddAttributeCommand,
    AddOperationCommand,
    AddRelationshipCommand,
    MoveNodeCommand,
    DeleteClassCommand {
    // marker interface para exhaustive pattern matching (Java 17+)
}
```

## Consecuencias

- **Positivo:** Un único punto de modificación del dominio. Defensa oral simple.
- **Positivo:** Agregar entrada por voz en Fase 8 no requiere modificar el dominio.
- **Positivo:** Alineado con la explicación del docente: "tres entradas distintas, un componente".
- **Negativo:** Requiere definir todos los tipos de comando. Compensado con `sealed interface`.

## Clasificación

`[DECISIÓN DE DISEÑO]` — Inspirado directamente en la explicación del docente sobre reutilización.

---

# ADR-005: Control Optimista de Concurrencia con @Version

**Fecha:** 2026-09-20
**Estado:** ACEPTADO
**Fase UP:** Elaboración

---

## Contexto

El docente menciona `[DOCENTE]` conceptos de exclusión mutua, sincronización y concurrencia. Necesitamos implementar alguna forma de control de conflictos desde las fases iniciales.

## Decisión

Usamos `@Version` de JPA en las entidades de persistencia para implementar **control optimista de concurrencia**.

**Terminología correcta** (importante para defensa oral):

| Mecanismo | Descripción |
|---|---|
| `@Version` (JPA) | Control **optimista** de concurrencia + detección de conflictos |
| Exclusión mutua | Bloqueado mientras otro proceso modifica (pesimista) — se implementa en Fase 3+ |

## Consecuencias

- **Positivo:** Detecta modificaciones concurrentes sin locks costosos.
- **Positivo:** Es explicable al docente correctamente, sin afirmar que `@Version` == exclusión mutua.
- **Pendiente:** La coordinación real de colaboración (serialización de comandos, broadcast) se trabaja en Fase 3.

## Clasificación

`[DECISIÓN DE DISEÑO]` — Complementario al requisito `[DOCENTE]` de concurrencia.
