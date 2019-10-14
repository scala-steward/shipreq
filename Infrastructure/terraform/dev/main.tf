module "env" {
  source = "../env"

  name               = "dev"
  env                = "dev"
  vpc_ip_prefix      = "10.0"
  availability_zone  = "us-east-2b"
  bastion_public_key = file("bastion_rsa.pub")
}

output "bastion_host" {
  value = module.env.bastion_host
}
