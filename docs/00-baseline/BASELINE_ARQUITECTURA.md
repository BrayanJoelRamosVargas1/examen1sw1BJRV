# 00 — Baseline del Proyecto
**Versión:** 0.2 | **Fecha:** 2026-09-20 | **Commit de referencia:** `74e054e`

---

## Propósito de este directorio

Este directorio `00-baseline` es el **punto de partida inmutable** del proyecto. Registra:

1. Las decisiones arquitectónicas acordadas **antes de escribir código**.
2. Las **desviaciones** cometidas respecto a lo acordado, con su corrección.
3. El estado del entorno de desarrollo real.

El commit `74e054e` es evidencia real del proceso. Se conserva íntegro en el historial de Git y **no se oculta ni se reescribe**. Las correcciones se registran como commits adicionales.

---

## Entorno Real de Desarrollo (detectado automáticamente)

| Herramienta | Versión detectada | Versión objetivo | Estado |
|---|---|---|---|
| Java | **17.0.16 (Temurin)** | **21 LTS** | ⚠️ `[PENDIENTE]` — ver §2 |
| Maven | 3.9.16 | 3.9.x | ✅ |
| Maven Wrapper | 3.3.4 | — | ✅ |
| Docker | 29.3.1 | — | ✅ |
| Docker Compose | v5.1.0 | — | ✅ |
| Node | v22.23.2 | — | ✅ |
| npm | 10.9.8 | — | ✅ |
| Git | 2.55.0 | — | ✅ |
| Angular CLI | 17.x | 17.x | ✅ |

---

## §1 — Decisiones Arquitectónicas Congeladas (no rediscutir)

| Decisión | Clasificación | Estado |
|---|---|---|
| Spring Boot para backend CASE | `[DECISIÓN DE DISEÑO]` | CONGELADO |
| Spring Boot para backend generado | `[DOCENTE]` | CONGELADO |
| Dominio UML puro (sin @Entity, sin Spring) | `[DECISIÓN DE DISEÑO]` | CONGELADO |
| Separación semántica UML / representación visual | `[DECISIÓN DE DISEÑO]` | CONGELADO |
| Commands en capa de aplicación (no en dominio) | `[DECISIÓN DE DISEÑO]` | CONGELADO |
| Puerto UmlModelRepository (interfaz en dominio) | `[DECISIÓN DE DISEÑO]` | CONGELADO |
| PostgreSQL vía Docker Compose | `[DECISIÓN DE DISEÑO]` | CONGELADO |
| GoJS como candidato principal de diagramación | `[PENDIENTE]` | NO CONGELADO |
| Angular 17 para frontend | `[DECISIÓN DE DISEÑO]` | CONGELADO |
| Proceso Unificado para documentación | `[DOCENTE]` | CONGELADO |
| @Version = control optimista (≠ exclusión mutua) | `[DECISIÓN DE DISEÑO]` | CONGELADO |

---

## §2 — Java: Entorno Actual vs. Objetivo

> [!IMPORTANT]
> **Java 21 sigue siendo la baseline objetivo hasta que nosotros decidamos cambiarla.**
> Java 17 es el entorno instalado actualmente. Es compatible con Spring Boot 3.2.x.
> El `pom.xml` declara `<java.version>17</java.version>` como adaptación al entorno real.

| Aspecto | Valor |
|---|---|
| Java instalado | 17.0.16 Temurin (verificado en consola) |
| Java objetivo del proyecto | **21 LTS** `[PENDIENTE — instalación]` |
| Acción requerida | Instalar Java 21, actualizar `pom.xml` a `<java.version>21</java.version>`, reverificar |
| Impacto en Fase 0 | Ninguno: el código generado es compatible con Java 17 y 21 |
| Cuándo se hace | Cuando el usuario instale Java 21 o así se acuerde explícitamente |

La diferencia relevante: con Java 21 podríamos usar **Pattern Matching for switch** (preview en 17, estable en 21) para el procesamiento de `UmlCommand`. Actualmente usamos `sealed interface` que funciona en ambas versiones.

---

## §3 — Desviaciones del Commit 74e054e (correcciones aplicadas)

Estas desviaciones fueron detectadas en la auditoría post-commit y **corregidas en el commit siguiente**. Se registran aquí como evidencia del proceso real de ingeniería.

| # | Desviación | Cómo se detectó | Corrección |
|---|---|---|---|
| D-01 | `UmlCommand.java` estaba en `domain/command` | Auditoría arquitectónica | Movido a `application/command` |
| D-02 | Estructura documental sin numeración (`inicio/elaboracion/...`) | Auditoría baseline | Renombrada a `00-baseline/01-inicio/02-elaboracion/03-construccion/04-transicion` |
| D-03 | Java 21 no documentado como objetivo vs. entorno real | Auditoría entorno | Registrado explícitamente en este documento (§2) |

> [!NOTE]
> Las desviaciones D-01 y D-02 no afectan la lógica de negocio ni los tests. Son correcciones de organización y arquitectura de paquetes.

---

## §4 — Criterios de Cierre de Fase 0 (checklist final)

| Criterio | Estado |
|---|---|
| `GET /api/health` responde 200 OK con PostgreSQL activo | ✅ Verificado — `{"status":"UP","phase":"FASE-0"}` |
| 14/14 tests de dominio pasando | ✅ `mvnw test -Dtest=UmlDomainTest` |
| `mvnw compile` BUILD SUCCESS | ✅ |
| Angular `npm run build` exitoso | ✅ Bundle 1.23 MB |
| `docker compose ps` — umlcase-db healthy | ✅ |
| Commit `74e054e` preservado en historial | ✅ No reescrito |
| `UmlCommand` en capa de aplicación | ✅ Corregido D-01 |
| Estructura documental numerada | ✅ Corregido D-02 |
| Java 21 registrado como objetivo | ✅ Registrado D-03 |
| Sin Fase 1 implementada | ✅ DETENIDO |
| Sin secrets en repositorio | ✅ |
| Sin node_modules en Git | ✅ |

---

## §5 — Regla de Control del Proyecto

A partir de aquí se aplica la siguiente regla operativa:

> **Ninguna instalación, commit, ni avance a Fase 1 sin autorización explícita.**

El criterio para avanzar es:

> **"YA ESTÁN DE ACUERDO. FASE 0 APROBADA. PODEMOS PASAR A FASE 1."**

Hasta recibir esa confirmación, el proyecto permanece en estado de auditoría.
