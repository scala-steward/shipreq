locals {
  node_exporter_tags = merge(var.default_tags, { Name = "${var.name_prefix}-node_exporter" })
}

resource "aws_ecs_service" "node_exporter" {
  name                = "${var.name_prefix}-node_exporter"
  cluster             = var.cluster_id
  task_definition     = aws_ecs_task_definition.node_exporter.arn
  scheduling_strategy = "DAEMON"
  propagate_tags      = "SERVICE"
  tags                = local.node_exporter_tags
}

resource "aws_ecs_task_definition" "node_exporter" {
  family = "${var.name_prefix}-node_exporter"
  tags   = local.node_exporter_tags

  container_definitions = <<EOB
[
  {
    "name": "${var.name_prefix}-node_exporter",
    "image": "${var.node_exporter_image}",
    "privileged": true,
    "mountPoints": [
      {
        "sourceVolume": "rootfs",
        "containerPath": "/rootfs",
        "readOnly": true
      },
      {
        "sourceVolume": "proc",
        "containerPath": "/host/proc",
        "readOnly": true
      },
      {
        "sourceVolume": "sys",
        "containerPath": "/host/sys",
        "readOnly": true
      }
    ],
    "portMappings": [
      {
        "protocol": "tcp",
        "hostPort": ${var.node_exporter_port},
        "containerPort": 9100
      }
    ],
    "cpu": ${var.node_exporter_cpu},
    "memoryReservation": ${var.node_exporter_mem_res},
    "memory": 92,
    "healthCheck": {
      "command": [
        "CMD-SHELL",
        "wget -qO - http://localhost:9100/metrics | fgrep -q '\"} ' || exit 1"
      ],
      "startPeriod": ${local.healthcheck.startPeriod},
      "interval": ${local.healthcheck.interval},
      "timeout": ${local.healthcheck.timeout},
      "retries": ${local.healthcheck.retries}
    }
  }
]
EOB

  volume {
    name      = "rootfs"
    host_path = "/"
  }

  volume {
    name      = "proc"
    host_path = "/proc"
  }

  volume {
    name      = "sys"
    host_path = "/sys"
  }
}
