# Created by ../global
data "aws_ecr_repository" "shipreq_base" {
  name = "shipreq/base"
}

# Created by ../global
data "aws_ecr_repository" "shipreq_ops_portal" {
  name = "shipreq/ops/portal"
}

# Created by ../global
data "aws_ecr_repository" "taskman" {
  name = "shipreq/taskman"
}

# Created by ../global
data "aws_ecr_repository" "webapp" {
  name = "shipreq/webapp"
}

resource "aws_ecr_repository" "shipreq_build" {
  name = "shipreq/build"
  tags = local.default_tags
}
