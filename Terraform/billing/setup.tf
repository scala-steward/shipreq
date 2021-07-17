terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.4"
    }
  }

  backend "s3" {
    bucket = "shipreq-terraform-state"
    key    = "billing.tfstate"
    region = "ap-southeast-2"
  }
}

provider "aws" {
  region = "ap-southeast-2"
}

provider "aws" {
  alias  = "cur" // cur = "Cost and Usage Report"
  region = "us-east-1"
}
