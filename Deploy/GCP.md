Create account.
Create different Projects for dev & prod (from UI).
gcloud auth login
gcloud projects list
gcloud config set project shipreq-dev

Git
===
git config credential.helper gcloud.sh
git remote add google https://source.developers.google.com/p/shipreq-dev/r/shipreq

Docker
======

docker tag shipreq/taskman asia.gcr.io/shipreq-dev/taskman
gcloud docker -- push asia.gcr.io/shipreq-dev/taskman

docker tag shipreq/taskman asia.gcr.io/shipreq-dev/taskman:2.0.0
gcloud docker -- push asia.gcr.io/shipreq-dev/taskman:2.0.0

docker tag shipreq/webapp asia.gcr.io/shipreq-dev/webapp
gcloud docker -- push asia.gcr.io/shipreq-dev/webapp

Terraform
=========

gcloud iam service-accounts create terraform --display-name 'Terraform service account'
gcloud iam service-accounts keys create .terraform-key.json --iam-account terraform@shipreq-dev.iam.gserviceaccount.com
gcloud projects add-iam-policy-binding shipreq-dev --member serviceAccount:terraform@shipreq-dev.iam.gserviceaccount.com --role roles/editor

Goals/Plan
==========

### Infra

Terraform and/or gcloud and/or deployment-manager
Parameterised by env
Run manually once per env

### Apps

* build
  * cloud builder watches git tag or invoke cloud builder from local cli
  * cloud builder applies sensible docker tags
* webapp
  * update kubernetes config in VCS; push
  * cloud builder monitors and updates cluster
* taskman
* update terraform config in VCS; push
* cloud builder monitors and updates VM

TODO!
* env
* secrets
