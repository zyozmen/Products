data "aws_ssm_parameter" "mongo_uri" {
  name = "/prod/products-service/MONGO_URI"
}
