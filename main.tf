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
    values = ["mongo-database-server"] # Asegúrate de que la EC2 tenga exactamente este Tag "Name"
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

# Security Group para la tarea en ECS
resource "aws_security_group" "ecs_sg" {
  name        = "products-api-ecs-sg"
  description = "Security Group para la API de productos en ECS"
  vpc_id      = data.aws_vpc.selected.id

  # Inbound: Permite tráfico al puerto 8080 (donde corre Spring Boot)
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
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
  security_group_id        = tolist(data.aws_instance.mongo_db.vpc_security_group_ids)[0] # <--- tolist() resuelve el error
  source_security_group_id = aws_security_group.ecs_sg.id                        # Autoriza a la tarea de ECS
  description              = "Permite trafico entrante desde ECS a MongoDB"
}

# ============================================================
# ECS CLUSTER, TASK DEFINITION & SERVICE
# ============================================================

resource "aws_ecs_cluster" "main" {
  name = "products-cluster"
}

resource "aws_ecs_task_definition" "app" {
  family                   = "products-api"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256" # 0.25 vCPU
  memory                   = "512" # 512 MB
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn

  container_definitions = jsonencode([
    {
      name      = "products-api"
      image     = "${aws_ecr_repository.products_service.repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "MONGO_HOST"
          value = data.aws_instance.mongo_db.private_ip # Inyecta automáticamente la IP privada de la EC2
        },
        {
          name  = "MONGO_PORT"
          value = "27017"
        }
      ]
    }
  ])
}

resource "aws_ecs_service" "app" {
  name            = "products-api-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.public.ids
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = true
  }

  depends_on = [
    aws_iam_role_policy_attachment.ecs_execution_policy
  ]
}