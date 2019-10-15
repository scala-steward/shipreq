locals {
  ops_tags = merge(local.default_tags, { Name = "${var.env} ops" })
}

resource "aws_ecs_cluster" "ops" {
  name = "${var.env}-ops"
  tags = local.ops_tags
}

resource "aws_autoscaling_group" "ops" {
  name                = "${var.env}-ops-cluster"
  min_size            = 1
  max_size            = 1
  desired_capacity    = 1
  vpc_zone_identifier = [aws_subnet.private-ops.id]
  tags                = [for k, v in local.ops_tags : { key = k, value = v, propagate_at_launch = true }]

  launch_template {
    id      = aws_launch_template.ops.id
    version = "$Latest"
  }
}

resource "aws_launch_template" "ops" {
  name                   = "${var.env}-ops-ecs"
  image_id               = data.aws_ssm_parameter.ami-ecs.value
  instance_type          = var.ops_instance_type
  vpc_security_group_ids = [aws_security_group.ops.id]
  tags                   = local.ops_tags

  # key_name
  # monitoring

  user_data = replace(
    base64encode(trimspace(templatefile("${path.module}/ops-ec2-init.sh", {
      cluster = aws_ecs_cluster.ops.name
    })))
  , "=", "")

  tag_specifications {
    resource_type = "instance"
    tags          = local.ops_tags
  }
  tag_specifications {
    resource_type = "volume"
    tags          = local.ops_tags
  }
}

resource "aws_security_group" "ops" {
  name   = "sg_${var.env}_ops"
  vpc_id = aws_vpc.main.id
  tags   = local.ops_tags

  ingress {
    protocol        = "tcp"
    from_port       = 22
    to_port         = 22
    security_groups = [aws_security_group.bastion.id]
  }
}
