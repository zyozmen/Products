# Configuración principal de Terraform para el despliegue de la imagen del servicio Products en AWS.
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

# Proveedor de AWS y región donde se desplegará la infraestructura.
provider "aws" {
  region = "us-east-2"
}

# Repositorio ECR para almacenar las imágenes Docker del servicio Products.
resource "aws_ecr_repository" "products_service" {
  name                 = "products-service"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  image_scanning_configuration {
    scan_on_push = true
  }
}

# Política de ciclo de vida del repositorio ECR para limpiar imágenes antiguas y no etiquetadas.
resource "aws_ecr_lifecycle_policy" "products_service_policy" {
  repository = aws_ecr_repository.products_service.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Eliminar imagenes sin tag (untagged) tras 1 dia"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 1
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Conservar solo las ultimas 2 imagenes con tag"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["v", "build-"]
          countType     = "imageCountMoreThan"
          countNumber   = 2
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}