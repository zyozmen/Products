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
