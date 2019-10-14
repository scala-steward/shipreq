Overview
========

* `init` - Terraform to setup an S3 bucket for all other Terraform to store its state.

* `global` - Terraform to setup environment-agnostic stuff.
             (eg. docker repos, proof of shipreq.com for 3rd party services)
             Depends on `init`.

* `cicd` - Terraform to setup CI/CD stuff.
           Depends on `init` and `global`.

* `env` - Configurable Terraform *module* to create a ShipReq runtime environment.
          Depends on `init` and `global`.

* `dev` - Terraform to setup a ShipReq dev environment
          Depends on `init` and `global`.


Initial Setup
=============

1. Create an AWS account
2. Have a user account and local env setup ([see](../AWS.md))
3. Terraform: `init`
4. Terraform: `global`
5. Terraform: `cicd`
6. CodeBuild: `shipreq_build`
7. CodeBuild: `shipreq_base`
8. CodeBuild: `shipreq`


Creating a Dev Environment
==========================

1. Terraform: `dev`
TODO : Bastion local setup