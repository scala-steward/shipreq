locals {

  default_tags = {
    createdBy = "terraform"
    env       = var.env
    terraform = "env-${var.env}"
    Name      = var.name
  }

  internal_domain    = "${var.env}.internal"
  internal_sd_domain = "${var.env}.sd.internal"

  nat_domain = "nat.${local.internal_domain}"

  prometheus_subdomain = "prometheus"
  prometheus_port      = 9090
  prometheus_host      = "${local.prometheus_subdomain}.${local.internal_sd_domain}"
  prometheus_url       = "http://${local.prometheus_host}:${local.prometheus_port}"

}
