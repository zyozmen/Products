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

data "aws_vpc" "selected" {
  default = true
}

data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.selected.id]
  }
}

# ============================================================
# ECR REPOSITORY & LIFECYCLE
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

resource "aws_security_group" "ecs_sg" {
  name        = "products-api-ecs-sg"
  description = "Security Group para la API de productos en ECS"
  vpc_id      = data.aws_vpc.selected.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ============================================================
# APPLICATION LOAD BALANCER (ALB)
# ============================================================

resource "aws_lb" "api" {
  name               = "products-api-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = data.aws_subnets.public.ids
}

resource "aws_lb_target_group" "api" {
  name        = "products-api-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.selected.id
  target_type = "ip"

  health_check {
    path                = "/api/productos/featured"
    matcher             = "200-399,404"
    interval            = 30
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 6
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.api.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

# ============================================================
# ECS CLUSTER
# ============================================================

resource "aws_ecs_cluster" "main" {
  name = "products-cluster"
}

# ============================================================
# CLOUDWATCH LOG GROUP
# ============================================================

resource "aws_cloudwatch_log_group" "ecs_log_group" {
  name              = "/ecs/products-service"
  retention_in_days = 7
}

# ============================================================
# ECS TASK DEFINITION Y SERVICE (CONECTADO A MONGO ATLAS)
# ============================================================

resource "aws_ecs_task_definition" "app" {
  family                   = "products-api"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn

  container_definitions = jsonencode([
    {
      name      = "products-api"
      image     = "${aws_ecr_repository.products_service.repository_url}:${var.image_tag}"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
          "awslogs-region"        = "us-east-2"
          "awslogs-stream-prefix" = "ecs"
        }
      }

      environment = [
        {
          name  = "SPRING_DATA_MONGODB_URI"
          # REEMPLAZA CON TU URI REAL DE MONGO ATLAS (Asegúrate de cambiar usuario y contraseña)
          value = "mongodb+srv://usuario_atlas:password_atlas@cluster0.mongodb.net/GrowShop?retryWrites=true&w=majority"
        },
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "prod"
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

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "products-api"
    container_port   = 8080
  }

  depends_on = [
    aws_iam_role_policy_attachment.ecs_execution_policy,
    aws_lb_listener.http
  ]
}

# ============================================================
# OUTPUTS
# ============================================================

output "alb_dns_name" {
  description = "DNS del ALB para usar como Origen en CloudFront"
  value       = aws_lb.api.dns_name
}