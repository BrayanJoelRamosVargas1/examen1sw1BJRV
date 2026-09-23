# Fase 5.0: Baseline de Interoperabilidad con Enterprise Architect

## 1. Objetivo y Alcance
El objetivo de esta fase es definir y congelar el contrato técnico y la estrategia arquitectónica para interoperar (importar y exportar) modelos UML con **Sparx Enterprise Architect**, cumpliendo con los requisitos docentes (REQ-08, REQ-09, REQ-09.1) sin acoplar nuestro dominio interno a tecnologías o formatos de intercambio específicos.

## 2. Auditoría de Documentación
Lo que exige la documentación actual:
* **GLOSARIO.md:** XMI es el formato estándar para interoperabilidad con EA.
* **MATRIZ_TRAZABILIDAD.md y REQUISITOS_DOCENTE.md:** Exigen explícitamente Import (REQ-08) y Export (REQ-09) hacia Enterprise Architect usando XMI (REQ-09.1).
* **REQUISITOS_DOCENTE.md (P-01):** Requiere definir la *versión exacta de XMI compatible con Enterprise Architect del laboratorio*.
* **ADR-001:** Indica que la entrada vía XMI debe producir comandos (`UmlCommand`) reutilizando el mismo pipeline que la edición manual.
* **ADR-002:** Afirma que *"exportar a XMI solo requiere leer UmlModel, no el layout"*. Esto establece explícitamente que la primera versión del export no requiere información visual.

## 3. Inventario y Subset UML Soportado
Nuestro modelo interno se mapea a los siguientes conceptos de UML 2.5:

| Modelo Interno | Elemento UML / XMI |
|---|---|
| `UmlClass` | `uml:Class` |
| `UmlAttribute` | `uml:Property` (dentro de `ownedAttribute`) |
| `UmlOperation` | `uml:Operation` (dentro de `ownedOperation`) |
| `UmlParameter` | `uml:Parameter` (dentro de `ownedParameter`) |
| `UmlRelationship` | `uml:Association` o `uml:Generalization` |
| `Visibility` | `visibility` (`public`, `private`, `protected`, `package`) |
| `UmlNodeView` | *Excluido del primer MVP (ver sección Layout)* |

## 4. Formato Seleccionado: Enterprise Architect XMI
No interactuaremos directamente con archivos de base de datos `.eap`, `.eapx` o `.qea`.
El formato de intercambio será **XMI (XML Metadata Interchange)**.

Target inicial de compatibilidad:
**XMI 2.1 / UML 2.x** compatible con Enterprise Architect.

Referencia inicial:
```xml
xmlns:xmi="http://schema.omg.org/spec/XMI/2.1"
xmlns:uml="http://schema.omg.org/spec/UML/2.1"
```

**[PENDIENTE DE VALIDACIÓN EA REAL]**
La estructura exacta del documento, wrappers, tipos primitivos, association ends y extensiones se congelarán contra un XMI exportado por la versión real de Enterprise Architect usada para la validación del proyecto. Las extensiones propietarias de Sparx se ignorarán inicialmente en importación y no se generarán en exportación, a menos que EA lo requiera.

## 4.1 Tipos Primitivos
Punto crítico para Fase 5.1: Se debe observar cómo la versión real de EA serializa:
* `String`
* `Integer`
* `Decimal`
* `Boolean`
* `void`

No asumir todavía que basta con atributos como `type="String"` porque XMI/UML puede representarlos mediante referencias a tipos internos o estándar OMG.

## 5. Política de Layout (Diagram Interchange)
Basado en el **ADR-002** ("exportar a XMI solo requiere leer UmlModel, no el layout"), la posición visual de los nodos (`UmlNodeView`, `x`, `y`, `width`, `height`) **queda excluida** del MVP (Opción B).
Intercambiar el diagrama visual requiere implementar *UML Diagram Interchange (UML DI)* o bien emitir el tag propietario `<elements><element xmi:idref="..." geometry="..."/></elements>` en la extensión de Enterprise Architect. Esto añade una complejidad innecesaria para el requisito docente principal (intercambio de modelo).

## 6. Mapping de Relaciones Semánticas
Las relaciones se representarán conceptualmente como:
* **ASSOCIATION**
* **GENERALIZATION**
* **AGGREGATION**
* **COMPOSITION**

**[VALIDAR CONTRA FIXTURE EA]**
La estructura XML concreta:
* `ownedEnd`
* `memberEnd`
* `aggregation="shared"`
* `aggregation="composite"`
* multiplicidades

Se debe validar cómo Enterprise Architect exporta y requiere estos constructos antes de congelar el serializador.

## 7. Política de IDs (`xmi:id`)
* **Regla XML:** Un `xmi:id` no puede comenzar con un número (los UUID frecuentemente lo hacen).
* **Estrategia Candidata (CASE → EA):** `EAID_<UUID normalizado>`. Se propone anteponer un prefijo constante a nuestros UUIDs para generar IDs compatibles y legibles.
* **[VALIDAR EN 5.1]:** Preservación/reescritura de `EAID_` por Enterprise Architect al importar y exportar.
* **Identidad estable:** Conceptualizamos que el `xmi:id` debe ser estable dentro del documento, las referencias internas deben apuntar al mismo `xmi:id`, y el UUID interno de nuestro dominio debe poder mapearse determinísticamente a partir de él.
* **Estrategia Import (EA → CASE):** Si leemos un ID, extraemos el UUID (si usa nuestro prefijo) o generamos uno determinista (hashing) / aleatorio para evitar colisiones.

## 8. Arquitectura y Puertos Propuestos
Se mantendrá el aislamiento absoluto del dominio.
**Frontera de aplicación (Ports):**
```text
application/
  port/
    in/
      ExportModelUseCase
      ImportModelUseCase
    out/
      UmlInterchangeExporter
      UmlInterchangeImporter
```
**Adaptadores (Infrastructure):**
```text
infrastructure/
  interchange/
    xmi/
      XmiExportAdapter
      XmiImportAdapter
```
El dominio (y la lógica central de Spring) **NO** importará DOM, JAXB, XMI ni clases de XML.

## 9. Política de Importación (Reemplazo vs Merge)
Para el primer MVP de importación (Fase 5.2):
* **Importación = Reemplazo Total (Replace).**
* El XML entrante sobrescribirá por completo las clases y relaciones del modelo, eliminando lo preexistente.
* **Concurrencia:** La importación se tratará como una única mutación masiva. Deberá proporcionar el `expectedVersion` y avanzará el `@Version` global en `N+1`. Si hay colisión (409), el cliente rechazará el archivo XMI y pedirá recargar. Una vez exitosa la importación, el backend emitirá el evento de commit, forzando a todos los clientes a recargar el snapshot completo (`GET /model`).

## 10. Seguridad del Parser XML
Cuando se implemente (Fase 5.2), el analizador XML (SAX/DOM) deberá cumplir obligatoriamente con:
* XXE disabled
* external entities disabled
* external DTD disabled
* size limits (límites razonables de tamaño)
* parser errors controlados

## 11. Roadmap de Implementación y Validación
La Fase 5 queda dividida en:

### 5.1 Export (CASE → EA)
Antes de implementar el exporter definitivo, se debe crear en Enterprise Architect un modelo mínimo:
```text
Package: PruebaInteroperabilidad
Clase Cliente
  - nombre: String
  + calcularTotal(cantidad: Integer): Decimal
Clase Pedido
  - total: Decimal
Relaciones:
  Cliente 1 -- * Pedido
```
Y si es posible, ejemplos separados de GENERALIZATION, AGGREGATION, COMPOSITION.
Se debe exportar desde EA como UML 2.x / XMI 2.1 y conservarlo como fixture de referencia en `backend/src/test/resources/xmi/ea/reference-minimal.xmi` SOLO cuando sea un archivo realmente exportado por EA. No se debe fabricar el fixture a mano.

**Validación 5.1:** El XML generado por nuestra herramienta debe abrirse sin errores en Enterprise Architect, mostrando clases, atributos, operaciones y asociaciones.

### 5.2 Import (EA → CASE)
Parsed de XMI exportado desde EA. **Validación:** EA exporta XMI → CASE lo carga y reemplaza el modelo de datos renderizando la UI.

### 5.3 Round-trip
(CASE → EA → CASE). Evaluar qué metadata se pierde (ej. layout).
