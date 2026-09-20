# Glosario del Proyecto
**Versión:** 0.1 | **Fecha:** 2026-09-20 | **Fase UP:** Inicio

---

## Términos del Dominio

| Término | Definición en este proyecto |
|---|---|
| **Herramienta CASE** | Computer-Aided Software Engineering. Software que asiste al ingeniero en actividades del proceso de desarrollo. Este proyecto **es** la herramienta CASE, no un sistema de gestión empresarial. |
| **UmlModel** | Modelo canónico interno que representa el estado semántico completo de un diagrama UML. Independiente de cualquier biblioteca de visualización. |
| **UmlClass** | Elemento del dominio que representa una clase UML con nombre, atributos y operaciones. **No** es una entidad JPA. |
| **UmlAttribute** | Propiedad de una UmlClass con nombre, tipo y visibilidad. |
| **UmlOperation** | Método de una UmlClass con nombre, tipo de retorno, visibilidad y parámetros. |
| **UmlRelationship** | Relación entre dos UmlClass. Tiene tipo (ASSOCIATION/GENERALIZATION/AGGREGATION/COMPOSITION/DEPENDENCY) y multiplicidades. |
| **UmlDiagram** | Representación visual de un UmlModel. Contiene nodos (UmlNodeView) con posición x/y en el canvas. Separado del modelo semántico. |
| **UmlNodeView** | Información de posición y tamaño de un elemento UML en el canvas. No altera el significado semántico del elemento. |
| **UmlCommand** | Objeto que encapsula una intención de modificación sobre el UmlModel (ej: CREATE_CLASS, ADD_ATTRIBUTE). Permite desacoplar la fuente de la modificación (manual, voz, imagen, XMI) de la lógica de dominio. |
| **CommandHandler** | Componente de la capa de aplicación que procesa un UmlCommand: valida, aplica al dominio y delega la persistencia al puerto correspondiente. |
| **Puerto (Port)** | Interfaz Java que define cómo el dominio o la aplicación interactúan con la infraestructura (persistencia, mensajería). El dominio no depende de la implementación. |
| **Adaptador (Adapter)** | Implementación concreta de un puerto. Por ejemplo: `UmlModelJpaRepository` implementa el puerto `UmlModelRepository`. |
| **Sala / Proyecto** | En este sistema, un "proyecto UML" es la unidad colaborativa. Los usuarios trabajan sobre el mismo UmlModel en tiempo real. |
| **Exclusión mutua** | `[PENDIENTE — Fase 3+]` Mecanismo que garantiza que solo un proceso modifica un recurso compartido a la vez. Se implementará en fases posteriores con serialización de comandos. |
| **Control optimista de concurrencia** | Técnica en la que múltiples transacciones proceden sin bloqueo, detectando conflictos al momento de persistir. Se implementa con `@Version` de JPA. **No** es exclusión mutua. |
| **XMI** | XML Metadata Interchange. Formato estándar de OMG para intercambiar modelos UML. Permite importar/exportar entre nuestra CASE y Enterprise Architect. `[PENDIENTE — Fase 5+]` |
| **Enterprise Architect** | Herramienta CASE comercial de Sparx Systems. Target de interoperabilidad mediante XMI. `[DOCENTE]` |
| **Spring Boot generado** | El backend Spring Boot que **genera** nuestra herramienta a partir del diagrama UML. Diferente del Spring Boot que **es** el backend de la propia herramienta CASE. |
| **Proceso Unificado** | Metodología de desarrollo de software iterativa e incremental adoptada para este proyecto. Sus fases son: Inicio, Elaboración, Construcción, Transición. `[DOCENTE]` |
| **ADR** | Architecture Decision Record. Documento que registra una decisión arquitectónica importante, su contexto, las alternativas consideradas y la justificación. |
