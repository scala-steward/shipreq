terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 2.31"
    }
  }
}

provider "aws" {
  region = "ap-southeast-2"
}
