locals {
  bastion_tags = merge(local.default_tags, { Name = "${var.env} bastion" })
}

data "aws_ami" "amazon-linux-2" {
  owners = ["amazon"]
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-2*-x86_64-gp2"]
  }
  most_recent = true
}

resource "aws_eip" "bastion" {
  vpc        = true
  instance   = aws_instance.bastion.id
  depends_on = [aws_internet_gateway.public]
  tags       = local.bastion_tags
}

resource "aws_security_group" "bastion" {
  name   = "sg_bastion"
  vpc_id = aws_vpc.main.id
  tags   = local.bastion_tags

  ingress {
    protocol    = "tcp"
    from_port   = 22
    to_port     = 22
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    protocol    = -1
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "bastion" {
  ami                         = data.aws_ami.amazon-linux-2.id
  associate_public_ip_address = false
  availability_zone           = var.availability_zone
  instance_type               = "t3a.nano"
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.bastion.id]
  key_name                    = aws_key_pair.bastion.key_name
  tags                        = local.bastion_tags

  user_data = templatefile("${path.module}/bastion-init.sh", {
    env = var.env
  })

  lifecycle { create_before_destroy = true }
}

resource "aws_key_pair" "bastion" {
  key_name   = "bastion-${var.env}"
  public_key = var.bastion_public_key
}
