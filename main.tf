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
    Name                                           = "vpc-products-prod"
    Environment                                    = "production"
    "kubernetes.io/cluster/products-cluster"       = "shared"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "igw-products-prod"
  }
}

# Subredes Públicas (Con tags requeridos por Kubernetes Ingress / ALB)
resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "us-east-2a"
  map_public_ip_on_launch = true

  tags = { 
    Name                                     = "subnet-public-1a"
    "kubernetes.io/cluster/products-cluster" = "shared"
    "kubernetes.io/role/elb"                 = "1"
  }
}

resource "aws_subnet" "public_b" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "us-east-2b"
  map_public_ip_on_launch = true

  tags = { 
    Name                                     = "subnet-public-1b"
    "kubernetes.io/cluster/products-cluster" = "shared"
    "kubernetes.io/role/elb"                 = "1"
  }
}

# Subredes Privadas (Para Nodos de K8s / Pods)
resource "aws_subnet" "private_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.10.0/24"
  availability_zone = "us-east-2a"

  tags = { 
    Name                                     = "subnet-private-1a"
    "kubernetes.io/cluster/products-cluster" = "shared"
    "kubernetes.io/role/internal-elb"        = "1"
  }
}

resource "aws_subnet" "private_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.11.0/24"
  availability_zone = "us-east-2b"

  tags = { 
    Name                                     = "subnet-private-1b"
    "kubernetes.io/cluster/products-cluster" = "shared"
    "kubernetes.io/role/internal-elb"        = "1"
  }
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

# S3 Gateway Endpoint (Ahorro de Costos)
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-2.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [aws_route_table.private.id]

  tags = { Name = "vpce-s3-gateway-prod" }
}

# ============================================================
# 2. AWS SSM PARAMETER STORE (SECRETOS)
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
# 3. ECR REPOSITORY & LIFECYCLE
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
# 4. CLÚSTER DE KUBERNETES (AWS EKS PROFESIONAL)
# ============================================================

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.15"

  cluster_name    = "products-cluster"
  cluster_version = "1.29"

  cluster_endpoint_public_access = true

  vpc_id     = aws_vpc.main.id
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]

  # Configuración de Nodos de Worker (Optimización de costos con Instancias Spot)
  eks_managed_node_groups = {
    spot_nodes = {
      min_size     = 1
      max_size     = 3
      desired_size = 2

      instance_types = ["t3.medium", "t3a.medium"]
      capacity_type  = "SPOT"

      labels = {
        Environment = "production"
        Workload    = "products-api"
      }
    }
  }

  # Configuración de accesos y seguridad
  manage_aws_auth_configmap = true

  tags = {
    Environment = "production"
    Terraform   = "true"
  }
}

# ============================================================
# OUTPUTS
# ============================================================

output "cluster_endpoint" {
  description = "Endpoint del Control Plane del clúster de EKS"
  value       = module.eks.cluster_endpoint
}

output "cluster_name" {
  description = "Nombre oficial del clúster EKS para Kubeconfig"
  value       = module.eks.cluster_name
}

output "nat_gateway_public_ip" {
  description = "IP PÚBLICA FIJA para agregar a la Whitelist de MongoDB Atlas"
  value       = aws_eip.nat.public_ip
}