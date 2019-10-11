variable "env" {
  description = "The short textual ID of this environment"
  type        = string
}

variable "name" {
  description = "The human-readable name (and optionally: desc) of this environment"
  type        = string
}

variable "vpc_ip_prefix" {
  description = "The first two IP4 values of the VPC"
  type        = string
}

variable "availability_zone" {
  type = string
}
