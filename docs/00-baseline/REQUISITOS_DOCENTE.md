# UML CASE Tool — Baseline de Requisitos
**Versión:** 0.1 | **Fecha:** 2026-09-20 | **Fase UP:** Inicio

---

## Leyenda de Clasificación

| Etiqueta | Significado |
|---|---|
| `[DOCENTE]` | Declarado explícitamente por el docente en audios/material |
| `[DECISIÓN DE DISEÑO]` | Decisión del equipo, justificable técnicamente |
| `[DECISIÓN DE DISEÑO — ALCANCE]` | Decisión de alcance del equipo |
| `[PENDIENTE]` | No está confirmado todavía; requiere verificación |

---

## 1. Definición del Producto

> **[DOCENTE]** La herramienta es una **Herramienta CASE colaborativa** para el modelado conceptual mediante **diagramas de clases UML**, con edición manual, por voz y mediante imagen, interoperabilidad con Enterprise Architect, generación automática de un backend Spring Boot ejecutable y mecanismos de IA orientados a mejorar la productividad.

> **[DOCENTE]** El producto **no** es un sistema de ventas, veterinaria, hospital, etc. Esos son dominios de prueba que el profesor entregará durante el examen. El software desarrollado es la **herramienta para ingenieros de software** (herramienta CASE).

---

## 2. Requisitos Funcionales Confirmados

| ID | Requisito | Clasificación |
|---|---|---|
| RF-01 | Crear diagramas de clases UML | `[DOCENTE]` |
| RF-02 | Edición manual del diagrama | `[DOCENTE]` |

| RF-03 | Edición mediante comandos de voz | `[DOCENTE]` |
| RF-04 | Crear modelo a partir de fotografía de diagrama | `[DOCENTE]` |
| RF-05 | Trabajo colaborativo entre múltiples usuarios | `[DOCENTE]` |
| RF-06 | Sincronización en tiempo real (no mediante F5) | `[DOCENTE]` |
| RF-07 | Manejo de concurrencia (exclusión mutua, sincronización) | `[DOCENTE]` |
| REQ-08 | Importar modelos desde Enterprise Architect | `[DOCENTE]` |
| REQ-09 | Exportar modelos hacia Enterprise Architect | `[DOCENTE]` |
| REQ-09.1 | XMI como formato de interoperabilidad | `[PENDIENTE / MECANISMO DE INTEROPERABILIDAD]` |
| RF-10 | Notación UML 2.5 o superior | `[DOCENTE]` |
| RF-11 | Generar backend Spring Boot ejecutable desde el diagrama | `[DOCENTE]` |
| RF-12 | Backend generado con PostgreSQL | `[DOCENTE]` |
| RF-13 | Transformación modelo OO → modelo relacional | `[DOCENTE]` |
| RF-14 | Backend generado: 4 capas mínimas (Entity/Repository/Service/Controller) | `[DOCENTE]` |
| RF-15 | DTO opcional en backend generado | `[DOCENTE]` |
| RF-16 | Backend generado completamente ejecutable (mvn spring-boot:run) | `[DOCENTE]` |
| RF-17 | Probar el backend generado | `[DOCENTE]` |
| RF-18 | IA integrada en el producto final | `[DOCENTE]` |
| RF-19 | Asistente para enseñar al usuario (dentro del software) | `[DOCENTE]` |
| RF-20 | Frontend móvil para la aplicación generada | `[DOCENTE]` |
| RF-20.1 | Desarrollo móvil con Flutter | `[PENDIENTE / CANDIDATO PRINCIPAL]` |
| RF-21 | Aplicación móvil funcional sin internet (offline) | `[DOCENTE]` |
| RF-22 | IA local en el dispositivo móvil | `[DOCENTE]` |
| RF-23 | Sincronización al recuperar conexión | `[DOCENTE]` |
| RF-24 | Despliegue en AWS | `[DOCENTE]` |

---

## 3. Alcance UML Soportado

> **[DECISIÓN DE DISEÑO — ALCANCE]** La herramienta trabajará con un **subconjunto de UML** orientado específicamente a diagramas de clases para modelado conceptual. No se afirma soporte a UML completo.

Elementos previstos:

| Elemento UML | Estado |
|---|---|
| Class | Previsto |
| Attribute / Property | Previsto |
| Operation / Method | Previsto |
| Parameter | Previsto |
| Association | Previsto |
| Generalization | Previsto |
| Aggregation | Previsto |
| Composition | Previsto |
| Dependency | Previsto (si el tiempo permite) |
| Multiplicity | Previsto |
| Visibility (public/private/protected/package) | Previsto |

---

## 4. Requisitos No Funcionales

| ID | Requisito | Clasificación |
|---|---|---|
| RNF-01 | Documentación según Proceso Unificado (Inicio/Elaboración/Construcción/Transición) | `[DOCENTE]` |
| RNF-02 | Fundamentación teórica aplicada (no relleno) | `[DOCENTE]` |
| RNF-03 | Correspondencia exacta documentación ↔ código | `[DOCENTE]` |
| RNF-04 | Código comprensible y defendible oralmente | `[DOCENTE]` |
| RNF-05 | Componentes reutilizables (misma operación, múltiples entradas) | `[DOCENTE]` |
| RNF-06 | Arquitectura de software justificable | `[DOCENTE]` |
| RNF-07 | Uso de IA durante el desarrollo (documentado) | `[DOCENTE]` |

---

## 5. Temas de Fundamentación Teórica Requeridos

> **[DOCENTE]** La fundamentación teórica debe aparecer **aplicada** en el software, no como definiciones copiadas.

1. Computer-Aided Software Engineering (CASE)
2. Desarrollo de software basado en componentes
3. Arquitectura de software
4. UML 2.5 o superior
5. Transformación / modelado objeto-relacional
6. Inteligencia Artificial en el desarrollo de software
7. Spring Boot

---

## 6. Reglas del Examen (Puertas Eliminatorias)

> **[DOCENTE]** El examen tiene **4 puertas consecutivas**. Fallar una impide continuar.

| Puerta | Criterio |
|---|---|
| 1 — Documentación | PDF publicado antes de las 08:00 del día del examen |
| 2 — Producto | Software terminado, probado y funcionando como producto |
| 3 — Correspondencia | Todo lo documentado existe en código y viceversa |
| 4 — Autoría | El equipo puede explicar, modificar y reconstruir cualquier módulo |

---

## 7. Lo que NO es Requisito del Docente

| Item | Clasificación |
|---|---|
| NestJS para el backend de la CASE | `[DECISIÓN DE DISEÑO]` — optamos por Spring Boot |
| GoJS como biblioteca de diagramación | `[PENDIENTE / CANDIDATO PRINCIPAL]` |
| Scrum como metodología | No — el docente exige Proceso Unificado |

---

## 8. Pendientes de Verificación

| ID | Pendiente |
|---|---|
| P-01 | Versión exacta de XMI compatible con Enterprise Architect del laboratorio |
| P-02 | Referencia bibliográfica "OMT" mencionada por el docente (¿Rumbaugh?) |
| P-03 | Licencia GoJS académica vs modo de evaluación |
| P-04 | Estrategia de herencia JPA para UmlClass (SINGLE_TABLE vs JOINED) |
