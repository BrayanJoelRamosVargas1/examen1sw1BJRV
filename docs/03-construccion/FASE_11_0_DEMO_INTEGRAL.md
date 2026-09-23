# Fase 11.0: Demo integral y matriz de evidencias

## Fixture canónico

`DemoUmlFixture` representa Persona, Cliente, Pedido, LineaPedido, Producto y Direccion, con atributos y relaciones de generalización, asociación, composición y agregación. `CaseToolEndToEndIT` usa el mismo modelo para verificar la cadena de interoperabilidad, persistencia relacional y generación.

## Aceptación automatizada

`CaseToolEndToEndIT` comprueba:

- equivalencia semántica tras exportar/importar XMI;
- generación y ejecución de DDL en PostgreSQL real mediante Testcontainers;
- generación de ZIP Spring Boot con `pom.xml`, entidades Java y `schema.sql`.

Las pruebas existentes cubren los endpoints de edición, concurrencia, STOMP, XMI, SQL y generación. Las interpretaciones de voz/texto/foto/asistente son read-only hasta que el usuario confirma el preview y reutilizan el batch secuencial.

## Matriz de trazabilidad

| Requisito | Implementación | Test/evidencia | Estado |
|---|---|---|---|
| Edición manual | REST + Angular | IT de handlers y smoke frontend | IMPLEMENTADO/VALIDADO |
| Colaboración realtime | STOMP + eventos AFTER_COMMIT | IT STOMP existentes | IMPLEMENTADO/VALIDADO |
| Optimistic concurrency | `expectedVersion` + 409 | tests de conflicto/resync | IMPLEMENTADO/VALIDADO |
| Voz determinista | Web Speech + parser | tests parser; micrófono manual pendiente | IMPLEMENTADO/PENDIENTE VALIDACIÓN REAL |
| Texto natural IA | fallback IA + preview | tests IA y `InterpretCommandApiIT` | IMPLEMENTADO/PENDIENTE PROVEEDOR REAL |
| Foto UML | multipart + fake multimodal + preview | `InterpretImageApiIT` | IMPLEMENTADO/PENDIENTE PROVEEDOR REAL |
| Asistente | analyzer local + IA opcional | `InterpretCommandApiIT` y tests assistant | IMPLEMENTADO/PENDIENTE PROVEEDOR REAL |
| XMI export | `XmiExportAdapter` | `ExportModelApiIT`, round-trip | IMPLEMENTADO/VALIDADO |
| XMI import | `XmiImportAdapter` | `ImportModelApiIT`, round-trip | IMPLEMENTADO/VALIDADO |
| XMI round-trip | equivalencia semántica normalizada | `CaseToolEndToEndIT`, `XmiRoundTripTest` | IMPLEMENTADO/VALIDADO |
| Modelo relacional | mapper relacional | tests de schema | IMPLEMENTADO/VALIDADO |
| PostgreSQL DDL | exporter SQL | `PostgreSqlDdlExecutionIT`, demo integral | IMPLEMENTADO/VALIDADO |
| Backend Spring Boot generado | ZIP exporter | `SpringBootProjectGeneratorTest`, demo integral | IMPLEMENTADO/VALIDADO |
| CRUD backend generado | proyecto generado | `SpringBootProjectGeneratorTest` y CRUD existente | IMPLEMENTADO/VALIDADO |
| Enterprise Architect físico | intercambio EA real | no hay evidencia reproducible | PENDIENTE |

## Resultado

La demo integral automatizada pasó con PostgreSQL real. No se ejecutó proveedor IA ni multimodal real y no se afirma esa evidencia. El micrófono físico y la validación física con Enterprise Architect permanecen pendientes.
