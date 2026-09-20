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

La herramienta acepta modificaciones al diagrama UML desde múltiples fuentes: manual, voz, imagen, XMI. El docente exige `[DOCENTE]` reutilización de componentes ("trabajen una vez, úsenlo muchas veces").

## Decisión

Toda modificación al `UmlModel` se expresa como un **`UmlCommand`**. Un `CommandHandler` centralizado procesa el comando independientemente de su origen.

```
Manual Input ─┐
Voice Input ──┼──► UmlCommand ──► CommandHandler ──► UmlModel ──► Port
Photo Input ──┘
XMI Input ────┘
```

Estructura del comando:

```java
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
