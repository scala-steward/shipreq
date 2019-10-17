locals {

  default_tags = {
    createdBy = "terraform"
    env       = var.env
    terraform = "env-${var.env}"
    Name      = var.name
  }

  internal_domain = "${var.env}.internal"

  prometheus_subdomain = "prometheus"
  prometheus_port      = 9090
  prometheus_host      = "${local.prometheus_subdomain}.${local.internal_domain}"
  prometheus_url       = "http://${local.prometheus_host}:${local.prometheus_port}"

}
