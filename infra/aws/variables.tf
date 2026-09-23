variable "aws_region" {
  description = "AWS Region for deployment"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (e.g. prod, dev)"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project base name"
  type        = string
  default     = "umlcase"
}

variable "db_username" {
  description = "Database username"
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "db_password" {
  description = "Database password"
  type        = string
  default     = "SuperSecurePassword123!" # Change for real deployments
  sensitive   = true
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway for private subnets. Set to false to save costs if Fargate tasks are placed in public subnets for demo."
  type        = bool
  default     = false
}
