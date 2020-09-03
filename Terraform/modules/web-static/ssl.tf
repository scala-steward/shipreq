resource "aws_acm_certificate" "web" {
  domain_name               = var.dns_domain
  provider                  = aws.us_east_1 // https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudfront_distribution#acm_certificate_arn
  subject_alternative_names = ["www.${var.dns_domain}"]
  tags                      = local.default_tags
  validation_method         = "DNS"

  lifecycle { create_before_destroy = true }
}

resource "aws_route53_record" "cert_validation" {
  for_each = {
    for o in aws_acm_certificate.web.domain_validation_options : o.domain_name => {
      name   = o.resource_record_name
      record = o.resource_record_value
      type   = o.resource_record_type
    }
  }

  name    = each.value.name
  type    = each.value.type
  records = [each.value.record]
  zone_id = var.dns_zone_id
  ttl     = 10800
}

resource "aws_acm_certificate_validation" "cert" {
  provider                = aws.us_east_1 // https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudfront_distribution#acm_certificate_arn
  certificate_arn         = aws_acm_certificate.web.arn
  validation_record_fqdns = [for r in aws_route53_record.cert_validation : r.fqdn]
}
