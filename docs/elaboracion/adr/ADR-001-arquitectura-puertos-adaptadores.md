# ADR-001: Arquitectura de Puertos y Adaptadores para el Dominio UML

**Fecha:** 2026-09-20
**Estado:** ACEPTADO
**Fase UP:** Elaboración

---

## Contexto

La herramienta CASE debe soportar múltiples formas de entrada (manual, voz, imagen, XMI) que todas producen el mismo resultado: modificaciones al modelo UML. Adicionalmente, el docente puede pedir durante el examen que expliquemos y modifiquemos cualquier parte del código.

## Decisión

Adoptamos una **arquitectura de puertos y adaptadores (Hexagonal)** para el módulo de dominio UML:

```
DOMINIO UML (puro, sin frameworks)
UmlModel / UmlClass / UmlRelationship / UmlAttribute / UmlOperation
          │
          ▼
APLICACIÓN
UmlCommand / CommandHandler
          │
          ▼ (mediante interfaz/puerto)
INFRAESTRUCTURA
JPA / PostgreSQL / WebSocket
```

El **dominio no importa** ninguna clase de Spring, JPA, Hibernate ni GoJS.

## Alternativas Consideradas

| Alternativa | Razón de rechazo |
|---|---|
| Entidades JPA directamente en dominio | Acopla el modelo al ORM; dificulta testear el dominio y defender cambios oralmente |
| Active Record pattern | El docente pide Clean Architecture justificable; AR mezcla dominio e infraestructura |
| Modelo anémico + servicios sin capas | No muestra conocimiento de arquitectura ante el docente |

## Consecuencias

- **Positivo:** El dominio es testeable sin base de datos. Se puede cambiar PostgreSQL por otro motor sin tocar el dominio.
- **Positivo:** Cualquier entrada (voz/imagen/XMI) produce el mismo `UmlCommand`, que es procesado por el mismo `CommandHandler`. Esto demuestra reutilización de componentes `[DOCENTE]`.
- **Negativo:** Más archivos iniciales. Justificable porque el docente espera arquitectura seria.

## Clasificación

`[DECISIÓN DE DISEÑO]` — El docente pide arquitectura justificable, no especificó Hexagonal explícitamente.
