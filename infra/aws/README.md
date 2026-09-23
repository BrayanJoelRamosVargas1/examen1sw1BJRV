# Infraestructura AWS (Fase 14.0)

Este directorio contiene los scripts de Terraform para desplegar la Arquitectura de Producción Propuesta (ALB + ECS Fargate + RDS).

> **ADVERTENCIA: `terraform apply` CREA RECURSOS FACTURABLES EN AWS.**
> No ejecutes `apply` a menos que estés seguro de los costos o vayas a utilizar el entorno de demo (EC2 + Docker Compose) documentado en Fase 14.1.

## Comandos Útiles

- **`terraform init`**: Inicializa el directorio de trabajo (descarga el provider de AWS). Usa `-backend=false` si solo vas a validar sintaxis.
- **`terraform fmt`**: Formatea el código HCL.
- **`terraform validate`**: Valida la sintaxis del código.
- **`terraform plan`**: Muestra qué recursos se crearán.
- **`terraform apply`**: Crea los recursos (¡Facturable!).
- **`terraform destroy`**: Elimina todos los recursos creados por Terraform.

## Configuración
1. Copia `terraform.tfvars.example` a `terraform.tfvars` y modifica las contraseñas/variables.
2. Asegúrate de tener credenciales válidas en tu entorno (ej. `aws configure`).
