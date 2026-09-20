# ADR-002: Separación entre Semántica UML y Representación Visual

**Fecha:** 2026-09-20
**Estado:** ACEPTADO
**Fase UP:** Elaboración

---

## Contexto

Un diagrama UML tiene dos dimensiones que deben manejarse por separado:

1. **Semántica UML**: Qué significa cada elemento (clase Cliente tiene atributo nombre de tipo String).
2. **Representación visual**: Dónde está ese elemento en el canvas (x=100, y=200, width=150).

Mover una clase en la pantalla **no cambia lo que esa clase significa** dentro del modelo UML.

## Decisión

Mantenemos dos modelos separados:

```
SEMÁNTICA (UmlModel)          REPRESENTACIÓN VISUAL (UmlDiagram)
─────────────────────         ────────────────────────────────────
UmlClass                      UmlNodeView
  - id: UUID              ←── - elementId: UUID (referencia)
  - name: String              - x: Double
  - attributes[]              - y: Double
  - operations[]              - width: Double
                              - height: Double

UmlRelationship               UmlEdgeView
  - id: UUID              ←── - relationshipId: UUID (referencia)
  - type                      - waypoints[]
  - sourceClassId
  - targetClassId
```

El `UmlDiagram` referencia elementos por `id` pero no los contiene.

## Alternativas Consideradas

| Alternativa | Razón de rechazo |
|---|---|
| Posición dentro de UmlClass (x, y en la misma entidad) | Mezcla semántica y presentación; exportar a XMI o generar Spring Boot no debería cargar posiciones de pantalla |
| Dejar solo GoJS que gestione todo | Acopla el sistema a GoJS; si se cambia la biblioteca, se pierde el modelo |

## Consecuencias

- **Positivo:** Exportar XMI solo requiere leer `UmlModel`, no el layout.
- **Positivo:** Generar Spring Boot solo requiere leer `UmlModel`.
- **Positivo:** Se puede cambiar GoJS por otra biblioteca sin tocar el dominio.
- **Negativo:** Sincronizar layout entre colaboradores requiere broadcast separado del modelo.

## Clasificación

`[DECISIÓN DE DISEÑO]` — Recomendada por buenas prácticas de separación de responsabilidades (SRP). Facilita la defensa oral ante el docente.
