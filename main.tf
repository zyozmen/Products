# ============================================================
# TERRAFORM & PROVIDER CONFIGURATION
# ============================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "terraform-state-505231787824"
    key            = "products-service/terraform.tfstate"
    region         = "us-east-2"
    dynamodb_table = "terraform-locks"
  }
}

provider "aws" {
  region = "us-east-2"
}

# ============================================================
# VARIABLES
# ============================================================

variable "image_tag" {
  type        = string
  default     = "latest"
  description = "Tag de la imagen de ECR a desplegar"
}

# ============================================================
# ECR REPOSITORY & LIFECYCLE
# Unico recurso que le corresponde al ciclo de vida del codigo.
# Red, datos y cluster EKS viven en el repo infra-aws.
# ============================================================

resource "aws_ecr_repository" "products_service" {
  name                 = "products-service"
  image_tag_mutability = "MUTABLE"
  force_delete         = false

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "products_service_policy" {
  repository = aws_ecr_repository.products_service.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Eliminar imagenes sin tag tras 1 dia"
        selection    = { tagStatus = "untagged", countType = "sinceImagePushed", countUnit = "days", countNumber = 1 }
        action       = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "Conservar las ultimas 2 imagenes etiquetadas"
        selection    = { tagStatus = "tagged", tagPrefixList = ["v", "build-", "latest"], countType = "imageCountMoreThan", countNumber = 2 }
        action       = { type = "expire" }
      }
    ]
  })
}

# ============================================================
# OUTPUTS
# ============================================================

output "ecr_repository_url" {
  description = "URL del repositorio ECR usado por el pipeline de la app"
  value       = aws_ecr_repository.products_service.repository_url
}
