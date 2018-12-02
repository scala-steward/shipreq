provider "aws" {
  region = "${var.region}"
}

variable "region" {
  default = "ap-southeast-2"
}
