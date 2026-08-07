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

module "infra" {
  source = "./Terraform"

  image_tag     = var.image_tag
  mongo_database = var.mongo_database
}

output "alb_dns_name" {
  value = module.infra.alb_dns_name
}

output "nat_gateway_public_ip" {
  value = module.infra.nat_gateway_public_ip
}
