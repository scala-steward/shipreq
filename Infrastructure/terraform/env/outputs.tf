output "bastion_host" {
  value       = aws_eip.bastion.public_ip
  description = "The public hostname or IP of the bastion instance."
}
