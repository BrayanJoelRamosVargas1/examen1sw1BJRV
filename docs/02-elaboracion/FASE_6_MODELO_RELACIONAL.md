# FASE 6 — MODELO RELACIONAL Y EXPORTACIÓN DDL

## 1. Transformación UML a Relacional
- **Clases a Tablas**: Cada UmlClass persistente se mapea a una tabla relacional (RelationalTable). Se generan nombres en formato snake_case determinista.
- **Atributos a Columnas**: Cada UmlAttribute se mapea a RelationalColumn. Los tipos soportados son:
  - String → VARCHAR(255)
  - Integer → INTEGER
  - Decimal → NUMERIC(19,2)
  - Boolean → BOOLEAN
- **Llave Primaria (PK)**: Decisión técnica del generador. Todas las tablas incluyen automáticamente una llave primaria técnica id de tipo UUID.

## 2. Relaciones UML a Claves Foráneas
- **1:N (Uno a Muchos)**: Agrega una FK en la tabla destino apuntando a la tabla origen.
- **N:M (Muchos a Muchos)**: Crea una tabla intermedia (Join Table) con FKs a ambas tablas, actuando como PK compuesta.
- **1:1 (Uno a Uno)**: Agrega una FK en una de las tablas con un constraint de unicidad (UNIQUE CONSTRAINT).
- **Aggregation**: Relación N:1 o 1:N donde NO se aplica un borrado en cascada (ON DELETE CASCADE) a nivel DDL por defecto, porque los componentes tienen vida independiente.
- **Composition**: Relación estructural fuerte. Se implementa con ON DELETE CASCADE en las claves foráneas correspondientes.
- **Generalization (Herencia)**: Decisión técnica del generador. Se implementa mediante el patrón **JOINED**. La tabla hija recibe un PK que a su vez es FK (ON DELETE CASCADE) hacia la tabla padre.

## 3. Generación de DDL (Data Definition Language)
El módulo PostgreSqlDdlExporter procesa el modelo intermedio (RelationalSchema) y genera código SQL determinista compatible con PostgreSQL.
Para garantizar el determinismo en la salida generada (mismo UML = mismo SHA-256), las tablas, columnas, constraints y foreign keys son ordenadas explícitamente en el proceso de generación SQL.

## 4. Pruebas y Validación Real
Se ha implementado PostgreSqlDdlExecutionIT, una prueba de integración que utiliza **Testcontainers** para levantar un contenedor real de PostgreSQL. En este entorno se ejecuta el DDL generado a partir de un grafo UML completo (Persona, Cliente, Pedido, Producto, herencia, N:M, etc.) y se verifica contra information_schema la correcta creación física de las tablas, columnas y dependencias foráneas.
