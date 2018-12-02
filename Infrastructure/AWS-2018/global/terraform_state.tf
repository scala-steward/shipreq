terraform {
  backend "s3" {
    bucket = "shipreq-terraform-state"
    key    = "global.tfstate"
    region = "ap-southeast-2"
  }
}
