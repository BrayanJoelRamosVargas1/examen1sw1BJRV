# Fase 14.0 — Arquitectura de Despliegue en AWS

En esta fase se establecen las bases para el despliegue de la herramienta CASE en Amazon Web Services (AWS), considerando dos enfoques distintos dependiendo del contexto (producción real vs. demostración académica).

El objetivo es tener la infraestructura declarada como código (IaC) sin incurrir en costos prematuros, garantizando que el entorno esté "AWS-Ready".

---

## A. Arquitectura de Producción Propuesta (Declarada en Terraform)

Esta es la arquitectura recomendada para un entorno empresarial, escalable y tolerante a fallos. Se encuentra definida en la carpeta `infra/aws` como IaC (Infrastructure as Code) usando Terraform.

**Componentes:**
- **ALB (Application Load Balancer):** Gestiona y enruta el tráfico HTTP/WebSocket entrante hacia los contenedores.
- **Amazon ECS con AWS Fargate:** Orquesta los contenedores Docker del Frontend (Nginx + React) y Backend (Spring Boot) en modo serverless (sin gestionar EC2 subyacentes).
- **Amazon RDS (PostgreSQL):** Base de datos relacional administrada.
- **NAT Gateway (Opcional):** Para permitir que los contenedores en subredes privadas descarguen imágenes de Docker u otras dependencias de Internet de forma segura. Se ha dejado deshabilitado por defecto (`enable_nat_gateway = false`) para evitar costos fijos durante validaciones técnicas.

> [!CAUTION]
> **Esta arquitectura NO debe ser desplegada automáticamente para pruebas académicas debido a los costos fijos asociados** (especialmente Fargate, NAT Gateway, ALB y RDS). Su declaración en Terraform (`vpc.tf`, `ecs.tf`, `rds.tf`, `alb.tf`) sirve como diseño de referencia y validación técnica.

---

## B. Arquitectura Académica Económica (Para Fase 14.1)

Para la demostración final (defensa del proyecto o simulación del examen) donde se debe probar el funcionamiento real en un entorno cloud con un costo mínimo (incluso nulo si se aprovecha el AWS Free Tier), utilizaremos este enfoque.

**Componentes:**
- **1 Instancia EC2 (e.g. t3.micro/t2.micro):** Un único servidor virtual Linux accesible mediante una IP pública.
- **Docker Compose:** Orquesta todos los servicios localmente dentro de la misma EC2.
- **Frontend Container:** Nginx sirviendo la SPA de React (Angular referenciado alternativamente).
- **Backend Container:** Spring Boot ejecutándose en Java 17.
- **Database Container:** PostgreSQL 15 ejecutándose dentro de la misma EC2 mediante Docker.

> [!TIP]
> **Beneficios:**
> Esta arquitectura permite demostrar exitosamente todo el stack (Spring Boot, PostgreSQL, WebSockets/STOMP, Docker y acceso público desde Internet) minimizando drásticamente la factura de AWS, eliminando componentes costosos como el ALB, RDS, ECS/Fargate y el NAT Gateway.

### Instrucciones de Despliegue (Fase 14.1)
El despliegue de esta arquitectura se realizará utilizando la configuración local de `docker-compose.prod.yml` que ya se encuentra probada y preparada, subiendo los archivos a la EC2 y levantando los servicios con `docker-compose up -d`.
