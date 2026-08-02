# Definición de la infraestructura de ECS para desplegar el servicio Products en Fargate.

# Obtiene la VPC por defecto de AWS para reutilizarla en el despliegue.
data "aws_vpc" "default" {
  default = true
}

# Obtiene las subnets disponibles dentro de la VPC por defecto.
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Variable que permite seleccionar el tag de la imagen ECR a desplegar.
variable "image_tag" {
  type        = string
  default     = "v1.0.0"
  description = "Tag de la imagen de ECR a desplegar"
}

# Grupo de logs de CloudWatch para capturar la salida del contenedor.
resource "aws_cloudwatch_log_group" "ecs_log_group" {
  name              = "/ecs/products-service"
  retention_in_days = 7
}

# Security Group que permite el acceso al puerto 8080 de la aplicación.
resource "aws_security_group" "app_sg" {
  name        = "products-app-sg"
  description = "Allow traffic to the products application"
  vpc_id      = data.aws_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "allow_8080" {
  security_group_id = aws_security_group.app_sg.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 8080
  to_port           = 8080
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "allow_all_outbound" {
  security_group_id = aws_security_group.app_sg.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# Rol de IAM que ECS utilizará para ejecutar tareas y acceder a servicios de AWS.
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "products-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "ecs-tasks.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Cluster ECS donde se alojará el servicio Products.
resource "aws_ecs_cluster" "products_cluster" {
  name = "products-cluster"
}

# Definición de la tarea ECS con el contenedor, recursos y configuración de logs.
resource "aws_ecs_task_definition" "products_task" {
  family                   = "products-task"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name      = "products"
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
          name  = "SPRING_PROFILES_ACTIVE"
          value = "prod"
        }
      ]
    }
  ])
}

# Servicio ECS que mantiene un contenedor en ejecución con la configuración definida.
resource "aws_ecs_service" "products_service" {
  name            = "products-service"
  cluster         = aws_ecs_cluster.products_cluster.id
  task_definition = aws_ecs_task_definition.products_task.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    assign_public_ip = true
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.app_sg.id]
  }
}

# Salida con la URL del repositorio ECR para uso posterior en pipelines o despliegues.
output "ecr_repository_url" {
  value       = aws_ecr_repository.products_service.repository_url
  description = "URL del repositorio ECR"
}