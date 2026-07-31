terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-2"
}

resource "aws_ecr_repository" "products_service" {
  name                 = "products-service"
  image_tag_mutability = "IMMUTABLE" # Garantiza trazabilidad y previene sobreescrituras
  force_delete         = false       # Protección contra borrado accidental en entorno real

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
          tagPrefixList = ["v", "build-"] # Ajusta segun la convencion de tags de tu CI/CD
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