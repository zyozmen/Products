# Configuración principal de Terraform para el servicio Products en AWS.
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

variable "image_tag" {
  type        = string
  default     = "latest"
  description = "Tag de la imagen de ECR a desplegar"
}

provider "aws" {
  region = "us-east-2"
}

# ============================================================
# DATOS DE INFRAESTRUCTURA EXISTENTE
# ============================================================

# VPC por defecto (o cambia a la VPC específica donde está tu EC2)
data "aws_vpc" "selected" {
  default = true
}

# Subredes de la VPC para desplegar la tarea de ECS
data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.selected.id]
  }
}

# Búsqueda de la EC2 existente de MongoDB por Tag
data "aws_instance" "mongo_db" {
  filter {
    name   = "tag:Name"
    values = ["products-db-aws"] # Asegúrate de que la EC2 tenga exactamente este Tag "Name"
  }

  filter {
    name   = "instance-state-name"
    values = ["running"] # Garantiza que solo consulte la instancia que realmente está activa
  }
}

# ============================================================
# ECR REPOSITORY & LIFECYCLE
# ============================================================

resource "aws_ecr_repository" "products_service" {
  name                 = "products-service"
  image_tag_mutability = "MUTABLE" # Cambiado a MUTABLE si usas etiquetas como 'latest' en desarrollo
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
        description  = "Conservar las ultimas 2 imagenes etiquetadas"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["v", "build-", "latest"]
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

# ============================================================
# ROLES DE IAM PARA ECS
# ============================================================

# Rol de ejecución necesario para que ECS pueda descargar imágenes de ECR y emitir logs a CloudWatch
resource "aws_iam_role" "ecs_execution_role" {
  name = "products-api-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = { Service = "ecs-tasks.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution_policy" {
  role       = aws_iam_role.ecs_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ============================================================
# SECURITY GROUPS (RED Y ACCESO)
# ============================================================

# 1. Security Group exclusivo para el ALB (definido primero)
resource "aws_security_group" "alb_sg" {
  name        = "products-api-alb-sg"
  description = "Security Group para el ALB"
  vpc_id      = data.aws_vpc.selected.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 2. Security Group para la tarea en ECS
resource "aws_security_group" "ecs_sg" {
  name        = "products-api-ecs-sg"
  description = "Security Group para la API de productos en ECS"
  vpc_id      = data.aws_vpc.selected.id

  # Inbound: Solo permite tráfico que venga explícitamente desde el ALB
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  # Outbound: Permite todo el tráfico de salida (necesario para hablar con MongoDB en la EC2)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Regla para abrir el puerto 27017 en la EC2 permitiendo el Security Group de ECS
resource "aws_security_group_rule" "allow_ecs_to_mongo" {
  type                     = "ingress"
  from_port                = 27017
  to_port                  = 27017
  protocol                 = "tcp"
  security_group_id        = tolist(data.aws_instance.mongo_db.vpc_security_group_ids)[0]
  source_security_group_id = aws_security_group.ecs_sg.id
  description              = "Permite trafico entrante desde ECS a MongoDB"
}

# ... (Mantén aquí tus bloques de aws_lb, aws_lb_target_group, aws_lb_listener, ecs_cluster, log_group, task_definition y ecs_service tal como los tenías) ...

# ============================================================
# OUTPUTS (CORREGIDOS Y EN NIVEL RAÍZ)
# ============================================================

output "mongo_ec2_private_ip" {
  description = "IP privada recuperada para MongoDB"
  value       = data.aws_instance.mongo_db.private_ip
}

output "alb_dns_name" {
  description = "DNS del ALB para usar como Origen en CloudFront"
  value       = aws_lb.api.dns_name
}
