resource "aws_s3_bucket" "config" {
  bucket = "shipreq-${var.env}-config"
  acl    = "private"
  tags   = merge(local.default_tags, { Name = "${var.env}-config" })
  versioning {
    enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "config" {
  bucket                  = aws_s3_bucket.config.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_iam_policy" "s3_config_ro" {
  name = "${var.env}_s3_config_ro_policy"

  policy = <<EOB
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Resource": [
        "${aws_s3_bucket.config.arn}",
        "${aws_s3_bucket.config.arn}/*"
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
