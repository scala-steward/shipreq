provider "aws" {
  region  = "ap-southeast-2"
  version = "~> 3.4"
}

terraform {
  required_version = ">= 0.13"

  backend "s3" {
    bucket = "shipreq-terraform-state"
    key    = "cicd.tfstate"
    region = "ap-southeast-2"
  }
}

locals {
  default_tags = {
    createdBy = "terraform"
    env       = "n/a"
    terraform = "cicd"
  }
}
