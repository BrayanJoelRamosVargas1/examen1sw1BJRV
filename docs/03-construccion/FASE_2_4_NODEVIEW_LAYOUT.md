# Fase 2.4 - NodeView y Persistencia del Layout

## Decisión Arquitectónica Clave

La semántica del modelo UML (`UmlModel`) y la presentación visual (`UmlDiagramLayout`) están estrictamente separadas para evitar colisiones y no mezclar responsabilidades.

```text
SEMÁNTICA                 PRESENTACIÓN
UmlModel                  UmlDiagramLayout
modelVersion = 48         layoutVersion = 2
     │                           │
     │                     PUT NodeView
     │                           ▼
     │                     layoutVersion = 3
     │
modelVersion = 48
```

## Invariante de Lifecycle

Durante la fase de diagnóstico se descubrió que Hibernate inicializa el `@Version` de `JpaUmlDiagramLayoutEntity` en `0` al realizar un `INSERT`. 

**Invariante:** 
Todo proyecto persistido debe poseer una raíz `UmlDiagramLayout` con `version=0` antes de aceptar un `SaveNodeView`.

La migración **V5** garantiza esta condición para los proyectos existentes al momento de la migración, inicializando el layout base. 

Actualmente, **NO existe** creación dinámica de proyectos en el sistema. Cuando se implemente la creación de nuevos proyectos (ej. `CreateProject`), la creación del layout raíz con `version=0` deberá formar parte obligatoria de esa transacción.

## Implementación REST
- **GET `/api/projects/{projectId}/diagram`**: End-point de sólo lectura para consultar el estado del layout (coordenadas visuales de las clases).
- **PUT `/api/projects/{projectId}/diagram/nodes`**: End-point para guardar las nuevas coordenadas de una clase. Aplica control de concurrencia optimista (`LAYOUT_VERSION_CONFLICT`) de forma aislada e incrementa la `layoutVersion` exclusivamente.
