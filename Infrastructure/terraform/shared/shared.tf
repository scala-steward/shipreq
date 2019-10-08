provider "aws" {
  region  = "${var.region}"
  version = "~> 2.31"
}

variable "region" {
  default = "ap-southeast-2"
}
