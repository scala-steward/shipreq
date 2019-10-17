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


Env Details
===========

* One VPC per env
  * 3 subnets: public, private-{app,ops}
  * one private DNS `<env>.internal` only accessible from within the VPC

* Prometheus
  * DNS entry `prometheus.<env>.internal` points to all active Prometheus containers
  * port: 9090


Problems
========

If you get an error like this when running Terraform:

    module.shipreq.aws_service_discovery_service.prometheus: Destroying... [id=srv-vguuopieeeayrdco]

    Error: ResourceInUse: Service contains registered instances; delete the instances before deleting the service
    	status code: 400, request id: 092e9d0b-03d1-4bf5-b4fe-b4c031894c22

To workaround:

    terraform destroy -target=module.shipreq.aws_ecs_service.prometheus

Issue: https://github.com/terraform-providers/terraform-provider-aws/issues/4853
