Overview
========

* `init` - Terraform to setup an S3 bucket for all other Terraform to store its state.

* `global` - Terraform to setup environment-agnostic stuff.
             (eg. docker repos, proof of shipreq.com for 3rd party services)
             Depends on `init`.

* `cicd` - Terraform to setup CI/CD stuff.
           Depends on `init` and `global`.


Prerequisites
=============

1. Have a user account and local env setup (see ../AWS.md)
2. Apply the terraform in `./init`.
