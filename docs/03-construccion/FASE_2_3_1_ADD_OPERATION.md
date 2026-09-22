# FASE 2.3.1 - ADD OPERATION

## Componentes Implementados

### Backend
*   **Endpoint REST**: `POST /api/projects/{projectId}/classes/{classId}/operations`
*   **Migración Base de Datos**: `V4__add_uml_operations.sql`
*   **Entidades de Dominio**: `UmlOperation`, `UmlParameter`
    *   **Diseño de Parámetros**: Los parámetros se insertan atómicamente al crear la operación. **NO** existe un CRUD independiente de parámetros en esta fase.
    *   **Identificadores**: `operationId` y `parameterId` son generados en el backend (UUID).
    *   **Ordenamiento**: Tanto las operaciones dentro de una clase, como los parámetros dentro de una operación, poseen un `orderIndex` determinista persistido en BD.
*   **Control de Concurrencia**:
    *   Soporte `expectedVersion` para capturar divergencias, arrojando `ModelVersionConflictException`.
    *   Protegido por `@Version` (Optimistic Locking) en JPA en `UmlModelEntity`.
    *   La versión retornada (`modelVersion`) es la real proveída desde el modelo en base de datos.
*   **Protocolo STOMP**:
    *   Se emite `OperationAddedEvent` al STOMP topic en fase `AFTER_COMMIT` de JPA.
    *   El DTO del WebSocket de operación agregada transporta la versión incrementada del modelo.
*   **Reconstrucción de Grafo JPA**: `GET /model` reconstruye el snapshot entero, incluyendo `classes`, sus `operations` y sus `parameters` anidados, previniendo `MultipleBagFetchException` mediante el uso de `Set` (vía `LinkedHashSet`).

### Frontend
*   **Notación UML**: El renderizado se apega al formato CASE estándar:
    `+ procesarPedido(cantidad: Integer, precio: Decimal): Decimal`
*   **Flujo Colaborativo DEMO A/B**:
    *   Browser A envía petición en versión 32, recibiendo 33.
    *   Browser B procesa el evento STOMP `OPERATION_ADDED` de manera reactiva, agregando la operación e incrementando la versión a 33.
    *   Ambos clientes (`F5`) reconstruyen la versión idéntica al recargar, probando la integridad de base de datos.

### Política de Firmas / Sobrecarga (Overloading)
Se implementó una política explícita en `UmlClass` para el control de unicidad de operaciones:
*   Se evalúa la **firma**, la cual está compuesta por el nombre de la operación y el arreglo posicional de los tipos de sus parámetros (orden y número).
*   Se **permite** la sobrecarga:
    *   `buscar(id: UUID)` y `buscar(nombre: String)` son válidas y coexisten.
*   Se **deniega** la re-definición de firma:
    *   Si ya existe `buscar(id: UUID)`, una nueva operación `buscar(codigo: UUID)` es rechazada.
*   **Restricción de parámetros:** Dentro de una misma operación, los parámetros deben tener nombres únicos. Restringido por un índice de BD y validación de dominio.

## Validación y Pruebas
*   **Backend**: 75 tests (Unit y de Integración con TestContainers), 0 failures, 0 errors, `BUILD SUCCESS`.
*   **Frontend Angular**: `npm run build` SUCCESS, bundle generado correctamente.
