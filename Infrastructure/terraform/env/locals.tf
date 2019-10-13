locals {
  default_tags = {
    createdBy = "terraform"
    env       = var.env
    terraform = "env-${var.env}"
    Name      = var.name
  }
}
