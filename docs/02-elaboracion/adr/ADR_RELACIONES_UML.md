# ADR: Modelo Conceptual de Relaciones UML

## Contexto y Objetivo
La Fase 3 incorpora el soporte para relaciones UML. Para evitar que la introducción de conectores visuales acople la lógica espacial con el dominio semántico de la herramienta, se establece este Architectural Decision Record (ADR) que gobernará el diseño de `UmlRelation`.

---

## 1. Principio Arquitectónico

Se consolida la siguiente estructura:
```text
UmlModel
├── UmlClass
└── UmlRelation

UmlDiagramLayout
└── UmlNodeView
```

Una **relación UML** pertenece estricta y únicamente al **modelo semántico**.

Por tanto:
- **Add/Update/Remove Relation** → Incrementa `modelVersion`. **NO** incrementa `layoutVersion`.
- **Move Node** (y futuro enrutamiento visual) → Incrementa `layoutVersion`. **NO** incrementa `modelVersion`.

> [!IMPORTANT]
> Queda **ESTRICTAMENTE PROHIBIDO** incluir coordenadas, waypoints o cualquier metadato de geometría dentro de la entidad `UmlRelation`.

---

## 2. Tipos Soportados

El subconjunto inicial de relaciones queda restringido a las siguientes (suficientes para el modelado conceptual requerido):

```java
enum RelationType {
    ASSOCIATION,
    AGGREGATION,
    COMPOSITION,
    GENERALIZATION
}
```

No se soportará inicialmente: Dependency, Realization, AssociationClass ni n-ary association.

---

## 3. Modelo Conceptual

La estructura de datos de una relación será la siguiente:

```text
UmlRelation
├── id: UUID
├── type: RelationType
├── sourceClassId: UUID
├── targetClassId: UUID
├── name: String?
├── sourceRole: String?
├── targetRole: String?
├── sourceMultiplicity: Multiplicity?
└── targetMultiplicity: Multiplicity?
```

El `id` es un identificador estable. Cualquier operación de *Update* futuro debe conservar obligatoriamente el `relationId`.

---

## 4. Semántica de Source / Target

Para evitar ambigüedades en la persistencia y futuro dibujado en el frontend, la orientación se define así:

- **ASSOCIATION:** `source` / `target` = extremos de la asociación.
- **AGGREGATION:** `source` = TODO, `target` = PARTE (el rombo vacío ◇ se dibuja en `source`).
- **COMPOSITION:** `source` = TODO, `target` = PARTE (el rombo lleno ◆ se dibuja en `source`).
- **GENERALIZATION:** `source` = SUBCLASE, `target` = SUPERCLASE (la flecha vacía △ apunta a `target`).

---

## 5. Multiplicidades

No se almacenarán como enteros separados o cadenas arbitrarias sin validación. Se definirá un *Value Object* conceptual `Multiplicity` basado en la siguiente gramática mínima:
- `*`
- `n`
- `n..m`
- `n..*`

Donde `n >= 0` y `m >= n`.

Si `sourceMultiplicity = null` o `targetMultiplicity = null`, esto significa estrictamente **"multiplicidad no especificada"**, y NO "multiplicidad implícita 1". El sistema no inventará una multiplicidad que el usuario no haya definido.

Para `GENERALIZATION`, las propiedades de `multiplicities = null` y `roles = null` **obligatoriamente**, dado que no aplican a la herencia.

---

## 6. Reglas del Dominio

- `sourceClassId` y `targetClassId` **deben existir**.
- **ASSOCIATION:** `source == target` **PERMITIDO** (asociación reflexiva).
- **AGGREGATION:** `source == target` **NO PERMITIDO** (restricción deliberada del subconjunto soportado por la herramienta).
- **COMPOSITION:** `source == target` **NO PERMITIDO** (restricción deliberada del subconjunto soportado por la herramienta).
- **GENERALIZATION:**
  - `source == target` **NO PERMITIDO** (no puede heredar de sí misma).
  - Impedir herencia bidireccional (`A → B` y `B → A`).
  - Impedir cualquier ciclo de herencia (`A → B → C → A`).
  - *Multiple inheritance* queda **PERMITIDA**, salvo que una restricción del docente exija lo contrario.

---

## 7. Manejo de Duplicados

**NO se creará una restricción SQL `UNIQUE(sourceClassId, targetClassId)`**. UML permite tener múltiples asociaciones semánticamente distintas entre el mismo par de clases.

Sin embargo, para **GENERALIZATION** sí debe impedirse la existencia de duplicados exactos (misma `source`, misma `target`, mismo `type`), mediante una regla de validación en la lógica de dominio.

---

## 8. Persistencia Futura

La Fase 3.1 creará la migración de base de datos (`V6__add_uml_relations.sql`) modelando la siguiente estructura conceptual:

```text
uml_relations
├── id UUID PK
├── model_id UUID FK
├── relation_type VARCHAR
├── source_class_id UUID FK
├── target_class_id UUID FK
├── name VARCHAR NULL
├── source_role VARCHAR NULL
├── target_role VARCHAR NULL
├── source_multiplicity VARCHAR NULL
└── target_multiplicity VARCHAR NULL
```

- Ambas clases involucradas deben pertenecer al mismo `UmlModel`.
- **No se modificarán** las migraciones históricas `V1` a `V5`.

---

## 9. Versionado Semántico

Toda mutación en las relaciones utilizará el versionado semántico ya existente:
- Validar `expectedVersion`.
- Depender de `UmlModel @Version`.
- Usar `markModified()` si es necesario forzar la versión.

Flujo esperado de `Add Relation`: `model N` → `model N+1`, mientras que `layout L` → sigue en `L`.
El motor de Hibernate continúa siendo el dueño absoluto de las iteraciones de la versión.

---

## 10. Snapshot y GET /model

El endpoint semántico `GET /model` evolucionará para incluir el listado de relaciones:
```json
{
  "version": 51,
  "classes": [...],
  "relations": [...]
}
```
Por diseño, `GET /diagram` **NO** debe devolver datos de relaciones semánticas.

---

## 11. Eventos STOMP

Se reutilizará el canal semántico existente: `/topic/projects/{projectId}`.
Los nuevos eventos seguirán la misma estructura (ej. `RELATION_ADDED`, `RELATION_UPDATED`, `RELATION_REMOVED`), conteniendo `commandId`, `projectId`, `relationId`, `modelVersion` y el payload necesario.

Las reglas de convergencia del cliente se mantienen idénticas:
- `event.modelVersion <= currentModelVersion` → Ignorar
- `event.modelVersion == currentModelVersion + 1` → Aplicar mutación
- `event.modelVersion > currentModelVersion + 1` → Solicitar Snapshot (`GET /model`)

---

## 12. Representación Visual (Frontend)

Durante la subfase 3.1 no se persistirá ninguna geometría de líneas en el backend. El frontend dibujará conectores de forma transitoria calculando la intersección visual: `source NodeView` → `target NodeView`.
Si un nodo se mueve, la línea se recalcula dinámicamente en el DOM/Canvas sin disparar mutaciones semánticas.

> [!NOTE]  
> Si en el futuro es requerido soportar *waypoints*, *bend points* o *etiquetas con posicionamiento manual*, estos datos pertenecerán invariablemente al agregado visual de `UmlDiagramLayout`, nunca a `UmlRelation`.

---

## 13. Roadmap Interno de Fase 3

El orden de trabajo propuesto es el siguiente:
1. `3.0` ADR Relaciones UML (Este documento)
2. `3.1` Add Relation → infraestructura base de UmlRelation, persistencia V6, y Add ASSOCIATION colaborativo
3. `3.2` Update Relation
4. `3.3` Remove Relation
5. `3.4` Render visual de conectores
6. `3.5` Generalization → habilitación / reglas especiales de GENERALIZATION
7. `3.6` Aggregation + Composition → habilitación / reglas especiales de AGGREGATION y COMPOSITION
8. `3.7` Multiplicidades y roles
9. `3.8` Demo integral Relaciones

*Nota:* `RelationType` declarará internamente los cuatro tipos desde el inicio, pero eso NO significa que 3.1 deba exponer funcionalmente todos los tipos. El orden podrá ser reajustado si surge una dependencia real, pero se prohíbe explícitamente implementar todo de golpe.
