resource "aws_ecr_repository" "base" {
  name = "shipreq/base"
}

resource "aws_ecr_repository" "taskman" {
  name = "shipreq/taskman"
}

resource "aws_ecr_repository" "webapp" {
  name = "shipreq/webapp"
}
