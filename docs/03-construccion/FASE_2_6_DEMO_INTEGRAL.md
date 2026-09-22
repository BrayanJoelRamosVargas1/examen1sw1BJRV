# Fase 2.6: Demo Integral de la Fase 2

Este documento recopila la evidencia de la demostración integral de las funcionalidades colaborativas construidas a lo largo de toda la **Fase 2** (Clases, Atributos, Operaciones y Layout).

## 1. Gate Automático

- **Backend Tests:** 100 tests ejecutados (0 Failures, 0 Errors, 0 Skipped). `BUILD SUCCESS`.
- **Frontend Build:** Generación exitosa de los bundles estáticos.
- **Auditoría de CSS (`app.component.css`):**
  - **Presupuesto Warning (`maximumWarning`):** 4.00 kB
  - **Presupuesto Error (`maximumError`):** 10.00 kB
  - **Tamaño Real:** 4.21 kB (provoca warning, pero no falla el build).
  - **Justificación:** El leve incremento se debe a la estilización del grid absoluto `.diagram-canvas`, las coordenadas `.class-card` y los pseudo-selectores para estados de arrastre (`:active`, `cursor: grabbing`). Se mantendrá así dado que corresponde directamente a la funcionalidad vital del lienzo.

## 2. Secuencia Integral de Sincronización A/B

La siguiente tabla demuestra la coexistencia e independencia de los versionados semántico (`modelVersion`) y visual (`layoutVersion`).

| Acción (en Cliente A) | `modelVersion` | `layoutVersion` | Evento STOMP a B | Resultado A | Resultado B | Persistencia F5 (A y B) |
|:---|:---|:---|:---|:---|:---|:---|
| **0. Baseline Inicial** | `M` | `L` | *(ninguno)* | Conectado, versión `M/L` | Conectado, versión `M/L` | Ambas reconstruyen `M/L` |
| **1. Rename Class** | `M` → `M+1` | `L` → `L` | `CLASS_RENAMED` | Refleja cambio, `v(M+1)` | Actualiza sola, `v(M+1)` | Ambas reconstruyen `M+1/L` |
| **2. Add Attribute** | `M+1` → `M+2` | `L` → `L` | `ATTRIBUTE_ADDED` | Nuevo attr, `v(M+2)` | Agrega attr solo, `v(M+2)` | Ambas reconstruyen `M+2/L` |
| **3. Update Attribute** | `M+2` → `M+3` | `L` → `L` | `ATTRIBUTE_UPDATED` | Attr editado, `v(M+3)` | Edita attr solo, `v(M+3)` | Ambas reconstruyen `M+3/L` |
| **4. Remove Attribute** | `M+3` → `M+4` | `L` → `L` | `ATTRIBUTE_REMOVED` | Attr eliminado, `v(M+4)`| Elimina attr, `v(M+4)` | Ambas reconstruyen `M+4/L` |
| **5. Add Operation** | `M+4` → `M+5` | `L` → `L` | `OPERATION_ADDED` | Nueva oper, `v(M+5)` | Agrega oper, `v(M+5)` | Ambas reconstruyen `M+5/L` |
| **6. Update Operation** | `M+5` → `M+6` | `L` → `L` | `OPERATION_UPDATED` | Oper editada, `v(M+6)` | Edita oper, `v(M+6)` | Ambas reconstruyen `M+6/L` |
| **7. Remove Operation** | `M+6` → `M+7` | `L` → `L` | `OPERATION_REMOVED` | Oper eliminada, `v(M+7)`| Elimina oper, `v(M+7)` | Ambas reconstruyen `M+7/L` |
| **8. Move Node (Drag 1)** | `M+7` → `M+7` | `L` → `L+1` | `NODE_MOVED` | Nodo movido, Layout `L+1`| Mueve nodo solo, Layout `L+1`| Ambas reconstruyen `M+7/L+1` |
| **9. Move Node (Drag 2)** | `M+7` → `M+7` | `L+1` → `L+2` | `NODE_MOVED` | Nodo movido, Layout `L+2`| Mueve nodo solo, Layout `L+2`| Ambas reconstruyen `M+7/L+2` |

## 3. Invarianza de Versiones y Características Comprobadas

Se ha demostrado de forma fehaciente la siguiente invariancia:

- **Mutación UML (Semántica):**
  - `modelVersion` N → N+1
  - `layoutVersion` permanece L

- **Move Node (Visual):**
  - `layoutVersion` L → L+1
  - `modelVersion` permanece N

Además, durante toda la demostración constan los siguientes elementos:
- **STOMP semántico** (Propagación del modelo en tiempo real).
- **STOMP layout** (Propagación visual en tiempo real desacoplada).
- **Persistencia PostgreSQL** robusta.
- **Recuperación por F5** (Lectura en frío consistente).
- **MODEL_VERSION_CONFLICT** (Optimistic locking sobre dominio validado en test 100/100).
- **LAYOUT_VERSION_CONFLICT** (Optimistic locking sobre visualización validado en test 100/100).

## 4. Conclusión de la Fase 2

Se ha demostrado que la infraestructura subyacente soporta mutaciones complejas sobre entidades, previene colisiones limpiamente con códigos de estado diferenciados, recibe sincronizaciones en milisegundos, y preserva la sanidad arquitectónica (Diagram vs Model).
