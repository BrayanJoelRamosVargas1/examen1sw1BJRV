# Fase 2.2.2: Update Attribute (Colaborativo)

## Resumen Técnico
Se implementó la actualización de atributos de clases de UML de manera colaborativa, asegurando la consistencia mediante concurrencia optimista y sincronización en tiempo real vía STOMP.

## Endpoint REST
- **Ruta**: `PUT /api/projects/{projectId}/classes/{classId}/attributes/{attributeId}`
- **Request Body**: `UpdateAttributeRequest` (incluyendo nombre, tipo, visibilidad y `expectedVersion`).
- **Respuesta**: HTTP 200 OK devolviendo `UpdateAttributeResponse` (JSON).

## Backend
- **Flujo**: El controlador delega a `UpdateAttributeHandler`, que a su vez llama a `UmlModel.updateAttribute` y `UmlClass.updateAttribute`.
- **Preservación**: Se mantiene intacto el `attributeId` y el `orderIndex` del atributo. Únicamente se actualizan `name`, `type` y `visibility`.
- **Validaciones**: Se rechazan nombres duplicados (case-insensitive) dentro de la misma clase, excluyendo al propio atributo actualizado.
- **Concurrencia**: Se utiliza **Optimistic Locking** mediante el campo `@Version` de JPA en `JpaUmlModelEntity`. No se usa `PESSIMISTIC_WRITE` ni `SELECT FOR UPDATE`. `lockForMutation` es un nombre histórico; la estrategia real es concurrencia optimista.
- **Versión Persistida**: La respuesta HTTP devuelve la versión real del modelo persistido en la base de datos (`modelVersion = savedModel.getVersion()`), la cual se incrementa de `N` a `N+1`.
- **Manejo de Errores**: Si `expectedVersion` difiere de la versión actual, se lanza un `ModelVersionConflictException` que se traduce en un `HTTP 409 MODEL_VERSION_CONFLICT`.

## Eventos (AFTER_COMMIT)
- Al confirmarse la transacción en la base de datos, se publica un `AttributeUpdatedEvent` que STOMP transmite a `/topic/projects/{projectId}`.
- Este evento contiene la versión exacta del modelo guardado en DB (`AttributeUpdatedEvent.modelVersion`) y un identificador tipado para el cliente (`AttributeUpdatedEvent.eventType = ATTRIBUTE_UPDATED`).

## Frontend (Angular)
- **Autor A**: Llama al `PUT`, actualiza su estado local inmediatamente desde el JSON de respuesta HTTP (asignándose la versión `N+1` persistida).
- **Deduplicación**: Cuando al autor A le llega su propio broadcast de STOMP (`ATTRIBUTE_UPDATED`), este evalúa que la versión del evento es `<= localVersion`, por lo que lo descarta/deduplica.
- **Autor B**: Recibe el broadcast de STOMP. Como su versión local es `N` y recibe la `N+1`, aplica la actualización visualmente de forma limpia sin necesitar `F5`.
- **Resync**:
  - Si se detecta un salto de versión (gap) a través de WebSocket.
  - Si llega un update sobre un `classId` o `attributeId` que no existe localmente.
  - Si un PUT arroja `HTTP 409` (conflicto).
  - En cualquiera de estos casos, el frontend hace un `GET /api/projects/{projectId}/model` (`loadModel()`) para resincronizar el snapshot completo desde PostgreSQL.

## Validación
- Pruebas Manuales (Demo A/B) verificaron la propagación del `ATTRIBUTE_UPDATED "mouseFinal" v14` sin recargar pantalla y la persistencia en DB (ambos clientes restauraron perfectamente tras `F5`).
- Pruebas automatizadas (60/60 verdes, 0 fallos, BUILD SUCCESS).
- Angular compiló de manera exitosa (Application bundle generation complete).
