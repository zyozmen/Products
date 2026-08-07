output "alb_dns_name" {
  description = "DNS del ALB para usar como Origen en CloudFront"
  value       = aws_lb.api.dns_name
}

output "nat_gateway_public_ip" {
  description = "IP PÚBLICA FIJA para agregar a la Whitelist / Network Access de MongoDB Atlas"
  value       = aws_eip.nat.public_ip
}
