# Fase 2.2.1: Add Attribute Colaborativo

## Evidencia Funcional Aprobada
- **POST /api/projects/{projectId}/classes/{classId}/attributes**: Implementado y funcional.
- **AddAttributeCommand**: Comando de aplicación implementado.
- **UmlModel.addAttribute(...)**: Lógica de dominio canónica implementada.
- **V3 / uml_attributes**: Migración Flyway aplicada exitosamente (V3__add_uml_attributes.sql).
- **orderIndex determinista**: El dominio asigna 0, 1, 2 correctamente al añadir atributos.
- **version N→N+1**: Hibernate actualiza la versión del modelo.
- **stale → 409**: El sistema intercepta conflictos y arroja 409 con ModelVersionConflictException estructurado.
- **ApiErrorResponse estructurado**: Estructura de respuesta de error corregida y validada.
- **AFTER_COMMIT**: Los eventos de integración y WebSocket solo se publican si la persistencia es exitosa en BD.
- **AttributeAddedEvent.modelVersion**: Evento incluye la versión del modelo actualizada.
- **STOMP A/B**: Demostrado funcional en el cliente (actualización reactiva de ambos navegadores).
- **GET /model con attributes[]**: Mapeo completo en lectura.
- **persistencia tras F5**: Los atributos persisten en PostgreSQL (BD dev).
- **54 tests verdes**: 54/54 tests en backend pasan exitosamente.
- **Angular build exitoso**: Frontend compila correctamente sin errores bloqueantes.

## Consideraciones Técnicas
- **`JpaUmlModelEntity.version` → `@Version`**: Usado por JPA para la concurrencia optimista (optimistic locking).
- **`last_modified` → dirty marker técnico**: Utilizado para forzar la actualización de versión del modelo al modificar entidades hijas, **NO** es `@Version`.
- No se afirma que `EntityGraph` garantice una sola consulta SQL, solo es una estrategia de optimización para reducir N+1 (JPA provee la implementación).
- Los eventos emitidos (ej. AttributeAddedEvent) no son "eventos de dominio" puros, son eventos de aplicación/colaboración.
