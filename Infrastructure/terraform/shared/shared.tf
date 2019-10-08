terraform {
  required_version = ">= 0.12"
}

provider "aws" {
  region  = var.region
  version = "~> 2.31"
}

variable "region" {
  default = "ap-southeast-2"
}

variable "shared_tags" {
  default = {
    "createdBy" = "terraform"
  }
}
