module "env" {
  source = "../env"

  name              = "dev"
  env               = "dev"
  vpc_ip_prefix     = "10.0"
  availability_zone = "us-east-2b"
}
