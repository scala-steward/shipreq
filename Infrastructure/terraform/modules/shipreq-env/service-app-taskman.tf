locals {
  shipreq_taskman_tags     = merge(local.default_tags, { Name = "${var.env}-shipreq-taskman" })
  s3_config_folder_taskman = "taskman"
}

resource "aws_ecs_service" "shipreq_taskman" {
  name                = "${var.env}-shipreq-taskman"
  cluster             = aws_ecs_cluster.app.id
  task_definition     = aws_ecs_task_definition.shipreq_taskman.arn
  scheduling_strategy = "DAEMON"
  propagate_tags      = "SERVICE"
  tags                = local.shipreq_taskman_tags
}

resource "aws_ecs_task_definition" "shipreq_taskman" {
  family        = "${var.env}-shipreq-taskman"
  task_role_arn = aws_iam_role.shipreq_taskman.arn
  tags          = local.shipreq_taskman_tags

  container_definitions = <<EOB
[
  {
    "name": "${var.env}-shipreq-taskman",
    "image": "${data.aws_ecr_repository.taskman.repository_url}:${var.shipreq_images_tag}",
    "environment": [
      {
        "name": "IMPORT_S3",
        "value": "s3://${aws_s3_bucket.config.bucket}/${local.s3_config_folder_taskman}"
      },
      {
        "name": "db.host",
        "value": "${local.postgres_domain}"
      },
      {
        "name": "db.database",
        "value": "${var.shipreq_db_name}"
      },
      {
        "name": "db.username",
        "value": "${var.shipreq_db_username}"
      },
      {
        "name": "db.password",
        "value": "${var.shipreq_db_password}"
      }
    ],
    "cpu": ${local.app_cluster_cpu.shipreq_taskman},
    "memoryReservation": ${local.app_cluster_mem_res.shipreq_taskman},
    "memory": 512
  }
]
EOB
}

resource "aws_s3_bucket_object" "taskman_properties" {
  bucket  = aws_s3_bucket.config.bucket
  key     = "${local.s3_config_folder_taskman}/conf/shipreq.properties"
  content = var.shipreq_taskman_properties
}

resource "aws_s3_bucket_object" "taskman_logback" {
  bucket  = aws_s3_bucket.config.bucket
  key     = "${local.s3_config_folder_taskman}/conf/logback.xml"
  content = var.shipreq_taskman_logback_xml
}

resource "aws_iam_role" "shipreq_taskman" {
  name = "${var.env}_ecs_shipreq_taskman"
  tags = local.shipreq_taskman_tags

  assume_role_policy = <<EOB
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Effect": "Allow",
      "Principal": { "Service": "ecs-tasks.amazonaws.com" }
    }
  ]
}
EOB
}

resource "aws_iam_role_policy_attachment" "shipreq_taskman_s3_config" {
  role       = aws_iam_role.shipreq_taskman.name
  policy_arn = aws_iam_policy.s3_config_ro.arn
}
