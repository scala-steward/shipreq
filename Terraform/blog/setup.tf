provider "aws" {
  region  = "ap-southeast-2" // Comment for blog/gatsy-config.ts
  version = "~> 3.4"
}

provider "aws" {
  alias   = "us_east_1"
  region  = "us-east-1"
  version = "~> 3.4"
}

terraform {
  required_version = ">= 0.13"

  backend "s3" {
    bucket = "shipreq-terraform-state"
    key    = "blog.tfstate"
    region = "ap-southeast-2"
  }
}

locals {
  default_tags = {
    createdBy = "terraform"
    env       = "n/a"
    terraform = "blog"
  }
}
