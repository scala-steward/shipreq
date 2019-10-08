resource "aws_s3_bucket" "terraform_state" {
  bucket = "shipreq-terraform-state"
  acl    = "private"

  versioning {
    enabled = true
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = merge(var.shared_tags, {
    "env" = "n/a"
  })
}
