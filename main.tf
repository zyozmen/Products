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
