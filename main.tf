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

variable "mongo_database" {
  type        = string
  default     = "GrowShop"
  description = "Nombre de la base de datos MongoDB"
}

variable "cluster_version" {
  type        = string
  default     = "1.30"
  description = "Versión de Kubernetes para el cluster EKS"
}

# ============================================================
# 1. RED (VPC, SUBREDES MULTI-AZ, NAT GATEWAY)
# ============================================================

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "vpc-products-prod"
    Environment = "production"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "igw-products-prod"
  }
}

# Subredes Públicas (Para ALB y NAT Gateway)
resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "us-east-2a"
  map_public_ip_on_launch = true

  tags = { Name = "subnet-public-1a" }
}

resource "aws_subnet" "public_b" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "us-east-2b"
  map_public_ip_on_launch = true

  tags = { Name = "subnet-public-1b" }
}

# Subredes Privadas (Para tareas de ECS / Backend)
resource "aws_subnet" "private_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.10.0/24"
  availability_zone = "us-east-2a"

  tags = { Name = "subnet-private-1a" }
}

resource "aws_subnet" "private_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.11.0/24"
  availability_zone = "us-east-2b"

  tags = { Name = "subnet-private-1b" }
}

# Elastic IP & NAT Gateway
resource "aws_eip" "nat" {
  domain     = "vpc"
  depends_on = [aws_internet_gateway.igw]

  tags = { Name = "eip-nat-gateway-prod" }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public_a.id

  tags       = { Name = "nat-gateway-prod" }
  depends_on = [aws_internet_gateway.igw]
}

# Tablas de Enrutamiento
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = { Name = "rt-public-prod" }
}

resource "aws_route_table_association" "public_a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "public_b" {
  subnet_id      = aws_subnet.public_b.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = { Name = "rt-private-prod" }
}

resource "aws_route_table_association" "private_a" {
  subnet_id      = aws_subnet.private_a.id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "private_b" {
  subnet_id      = aws_subnet.private_b.id
  route_table_id = aws_route_table.private.id
}

# ============================================================
# OPTIMIZACIÓN DE COSTOS: S3 GATEWAY ENDPOINT (GRATUITO)
# ============================================================

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-2.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [aws_route_table.private.id]

  tags = { Name = "vpce-s3-gateway-prod" }
}

# ============================================================
# 2. SECURITY GROUPS
# ============================================================

resource "aws_security_group" "alb_sg" {
  name        = "products-api-alb-sg"
  description = "Security Group para el ALB"
  vpc_id      = aws_vpc.main.id

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
  vpc_id      = aws_vpc.main.id

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
# 3. AWS SSM PARAMETER STORE (SECRETOS)
# ============================================================

resource "aws_ssm_parameter" "mongo_uri" {
  name      = "/prod/products-service/MONGO_URI"
  type      = "SecureString"
  value     = "placeholder"
  overwrite = false

  lifecycle {
    ignore_changes = [value]
  }
}

# ============================================================
# 4. ROLES DE IAM
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

resource "aws_iam_role_policy" "ecs_ssm_inline_policy" {
  name = "products-api-ssm-read-policy"
  role = aws_iam_role.ecs_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ssm:GetParameters", "ssm:GetParameter"]
        Resource = [aws_ssm_parameter.mongo_uri.arn]
      }
    ]
  })
}

# ============================================================
# 5. ECR REPOSITORY & LIFECYCLE
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
# 6. EKS CLUSTER PARA KUBERNETES
# ============================================================

resource "aws_iam_role" "eks_cluster_role" {
  name = "products-eks-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.eks_cluster_role.name
}

resource "aws_iam_role" "eks_node_group_role" {
  name = "products-eks-node-group-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_node_group_worker" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role       = aws_iam_role.eks_node_group_role.name
}

resource "aws_iam_role_policy_attachment" "eks_node_group_cni" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_node_group_role.name
}

resource "aws_iam_role_policy_attachment" "eks_node_group_ecr" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role       = aws_iam_role.eks_node_group_role.name
}

resource "aws_eks_cluster" "main" {
  name     = "products-cluster"
  role_arn = aws_iam_role.eks_cluster_role.arn
  version  = var.cluster_version

  vpc_config {
    subnet_ids              = [aws_subnet.public_a.id, aws_subnet.public_b.id, aws_subnet.private_a.id, aws_subnet.private_b.id]
    endpoint_private_access = true
    endpoint_public_access  = true
    public_access_cidrs     = ["0.0.0.0/0"]
  }

  depends_on = [aws_iam_role_policy_attachment.eks_cluster_policy]
}

resource "aws_eks_node_group" "general" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "general"
  node_role_arn   = aws_iam_role.eks_node_group_role.arn
  subnet_ids      = [aws_subnet.private_a.id, aws_subnet.private_b.id]

  scaling_config {
    desired_size = 1
    max_size     = 2
    min_size     = 1
  }

  capacity_type  = "ON_DEMAND"
  instance_types = ["t3.small"]

  depends_on = [
    aws_iam_role_policy_attachment.eks_node_group_worker,
    aws_iam_role_policy_attachment.eks_node_group_cni,
    aws_iam_role_policy_attachment.eks_node_group_ecr,
  ]
}

# ============================================================
# OUTPUTS
# ============================================================

output "eks_cluster_name" {
  description = "Nombre del cluster EKS para usar con kubectl"
  value       = aws_eks_cluster.main.name
}

output "eks_cluster_endpoint" {
  description = "Endpoint del cluster EKS"
  value       = aws_eks_cluster.main.endpoint
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR"
  value       = aws_ecr_repository.products_service.repository_url
}

output "nat_gateway_public_ip" {
  description = "IP PÚBLICA FIJA para agregar a la Whitelist de MongoDB Atlas"
  value       = aws_eip.nat.public_ip
}