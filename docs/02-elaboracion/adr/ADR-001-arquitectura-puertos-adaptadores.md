# ADR-001: Arquitectura de Puertos y Adaptadores para el Dominio UML

**Fecha:** 2026-09-20
**Estado:** ACEPTADO
**Fase UP:** Elaboración

---

## Contexto

El docente exige establecer una **Arquitectura de Software justificable**. La aplicación debe ser capaz de evolucionar y soportar múltiples clientes (API, Web, móvil) o formas de entrada (voz, XMI) sin acoplarse a tecnologías específicas desde el día cero.

## Decisión

Se adopta una **Arquitectura Hexagonal (Puertos y Adaptadores)**, inspirada en los principios de Clean Architecture `[DECISIÓN DE DISEÑO]` para el módulo de dominio UML:

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
| Active Record pattern | El docente pide arquitectura justificable, optamos por diseño limpio; AR mezcla dominio e infraestructura |
| Modelo anémico + servicios sin capas | No muestra conocimiento de arquitectura ante el docente |

## Consecuencias

- **Positivo:** El dominio es testeable sin base de datos. Se puede cambiar PostgreSQL por otro motor sin tocar el dominio.
- **Positivo:** Cualquier entrada (voz/imagen/XMI) produce el mismo `UmlCommand`, que es procesado por el mismo `CommandHandler`. Esto demuestra reutilización de componentes `[DOCENTE]`.
- **Negativo:** Más archivos iniciales. Justificable porque el docente espera arquitectura seria.

## Clasificación

`[DECISIÓN DE DISEÑO]` — El docente pide arquitectura justificable, no especificó Hexagonal explícitamente.
