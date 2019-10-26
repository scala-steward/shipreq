locals {
  shipreq_webapp_tags = merge(local.default_tags, { Name = "${var.env}-shipreq-webapp" })
}

resource "aws_s3_bucket" "shipreq_webapp" {
  bucket = "shipreq-${var.env}-shipreq-webapp"
  acl    = "private"
  tags   = local.shipreq_webapp_tags
  versioning {
    enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "shipreq_webapp" {
  bucket                  = aws_s3_bucket.shipreq_webapp.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_object" "keystore" {
  bucket = aws_s3_bucket.shipreq_webapp.bucket
  key    = "etc/keystore"
  source = var.shipreq_webapp_keystore_filename
}

resource "aws_s3_bucket_object" "ssl_passwords" {
  bucket = aws_s3_bucket.shipreq_webapp.bucket
  key    = "start.d/ssl-passwords.ini"
  source = var.shipreq_webapp_ssl_passwords_ini_filename
}

resource "aws_iam_policy" "shipreq_webapp_s3" {
  name = "${var.env}_shipreq_webapp_s3_policy"

  policy = <<EOB
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Resource": [
        "${aws_s3_bucket.shipreq_webapp.arn}",
        "${aws_s3_bucket.shipreq_webapp.arn}/*"
      ],
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ]
    }
  ]
}
EOB
}
