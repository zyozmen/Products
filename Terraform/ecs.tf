resource "aws_ecs_cluster" "main" {
  name = "products-cluster"
}

resource "aws_cloudwatch_log_group" "ecs_log_group" {
  name              = "/ecs/products-service"
  retention_in_days = 7
}

resource "aws_ecs_task_definition" "app" {
  family                   = "products-api"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
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
          name  = "SPRING_PROFILES_ACTIVE"
          value = "prod"
        }
      ]

      secrets = [
        {
          name      = "SPRING_DATA_MONGODB_URI"
          valueFrom = data.aws_ssm_parameter.mongo_uri.arn
        },
        {
          name      = "MONGO_URI"
          valueFrom = data.aws_ssm_parameter.mongo_uri.arn
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
    subnets          = [aws_subnet.private_a.id, aws_subnet.private_b.id]
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "products-api"
    container_port   = 8080
  }

  depends_on = [
    aws_iam_role_policy_attachment.ecs_execution_policy,
    aws_iam_role_policy_attachment.ecs_ssm_policy_attach,
    aws_lb_listener.http
  ]
}
