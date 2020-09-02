resource "aws_acm_certificate" "shipreq" {
  domain_name               = local.shipreq_domain
  validation_method         = "DNS"
  tags                      = local.default_tags
  subject_alternative_names = ["www.${local.shipreq_domain}", local.analytics_proxy_domain]

  lifecycle { create_before_destroy = true }
}

# DNS records for cert validation - keep in mind that this isn't the ALB endpoint record
resource "aws_route53_record" "cert_validation" {
  for_each = {
    for o in aws_acm_certificate.shipreq.domain_validation_options : o.domain_name => {
      name   = o.resource_record_name
      record = o.resource_record_value
      type   = o.resource_record_type
    }
  }

  name    = each.value.name
  type    = each.value.type
  records = [each.value.record]
  zone_id = local.shipreq_zone_id
  ttl     = 10800
}

resource "aws_acm_certificate_validation" "cert" {
  certificate_arn         = aws_acm_certificate.shipreq.arn
  validation_record_fqdns = [for r in aws_route53_record.cert_validation : r.fqdn]
}

# www.shipreq.com -> shipreq.com
resource "aws_route53_record" "www" {
  zone_id = local.shipreq_zone_id
  name    = "www.${local.shipreq_domain}."
  type    = "CNAME"
  ttl     = "21600"
  records = ["${local.shipreq_domain}."]
}
