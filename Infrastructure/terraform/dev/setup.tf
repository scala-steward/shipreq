provider "aws" {
  region  = "us-east-2"
  version = "~> 2.32"
}

provider "aws" {
  alias   = "ap-southeast-2"
  region  = "ap-southeast-2"
  version = "~> 2.32"
}

terraform {
  required_version = ">= 0.12"

  backend "s3" {
    bucket = "shipreq-terraform-state"
    key    = "env-dev.tfstate"
    region = "ap-southeast-2"
  }
}
