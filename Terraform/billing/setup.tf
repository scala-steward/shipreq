terraform {
  required_version = ">= 0.13"

  backend "s3" {
    bucket = "shipreq-terraform-state"
    key    = "billing.tfstate"
    region = "ap-southeast-2"
  }
}

provider "aws" {
  region  = "ap-southeast-2"
  version = "~> 3.4"
}

provider "aws" {
  alias   = "cur" // cur = "Cost and Usage Report"
  region  = "us-east-1"
  version = "~> 3.4"
}
