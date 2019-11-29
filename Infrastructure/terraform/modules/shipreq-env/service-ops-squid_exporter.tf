locals {
  squid_exporter_tags = merge(local.default_tags, { Name = "${var.env}-ops-squid_exporter" })
}

resource "aws_ecs_service" "squid_exporter" {
  name                = "${var.env}-ops-squid_exporter"
  cluster             = aws_ecs_cluster.ops.id
  task_definition     = aws_ecs_task_definition.squid_exporter.arn
  scheduling_strategy = "DAEMON"
  propagate_tags      = "SERVICE"
  tags                = local.squid_exporter_tags
}

resource "aws_ecs_task_definition" "squid_exporter" {
  family = "${var.env}-ops-squid_exporter"
  tags   = local.squid_exporter_tags

  container_definitions = <<EOB
[
  {
    "name": "${var.env}-ops-squid_exporter",
    "image": "${data.aws_ecr_repository.squid_exporter.repository_url}:${var.ops_images_tag}",
    "environment": [
      {
        "name": "SQUID_HOSTNAME",
        "value": "${local.nat_domain}"
      },
      {
        "name": "SQUID_PORT",
        "value": "${local.ports.nat.squid}"
      }
    ],
    "portMappings": [
      {
        "protocol": "tcp",
        "hostPort": ${local.ports.ops.squid_exporter},
        "containerPort": 9301
      }
    ],
    "cpu": ${local.ops_cluster_cpu.squid_exporter},
    "memoryReservation": ${local.ops_cluster_mem_res.squid_exporter},
    "memory": 32
  }
]
EOB
}
