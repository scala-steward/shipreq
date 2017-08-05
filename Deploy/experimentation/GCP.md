Create account.
Create different Projects for dev & prod (from UI).
sudo gcloud components install kubectl
sudo gcloud components install beta
sudo gcloud components install alpha
sudo gcloud components update
gcloud auth login
gcloud projects list
gcloud config set project shipreq-dev

############


> gcloud auth application-default login

Credentials saved to file: [/home/golly/.config/gcloud/application_default_credentials.json]

These credentials will be used by any library that requests
Application Default Credentials.


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

DM
==

./run
gcloud container clusters get-credentials apps --zone australia-southeast1-c --project shipreq-dev

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


=========================================
Taskman SQL access

Enable Cloud SQL Administration API - https://console.cloud.google.com/flows/enableapi?apiid=sqladmin&redirect=https://console.cloud.google.com&_ga=1.69095313.1440849751.1500257608


gcloud iam service-accounts create taskman --display-name 'Taskman service account'

gcloud projects add-iam-policy-binding shipreq-dev --member serviceAccount:taskman@shipreq-dev.iam.gserviceaccount.com --role roles/cloudsql.client

gcloud iam service-accounts keys create .taskman-key.json --iam-account taskman@shipreq-dev.iam.gserviceaccount.com

kubectl create secret generic taskman-credentials --from-file=credentials.json=.taskman-key.json

TASKMAN_DB_USERNAME=taskman
TASKMAN_DB_PASSWORD=$(pwgen 64 1)

gcloud sql users create $TASKMAN_DB_USERNAME host --instance=shipreq-db --password=$TASKMAN_DB_PASSWORD

# kubectl create secret generic taskman-db-props --from-literal=db.username=$TASKMAN_DB_USERNAME --from-literal=db.password=$TASKMAN_DB_PASSWORD

echo -n "db.username=$TASKMAN_DB_USERNAME\ndb.password=$TASKMAN_DB_PASSWORD\n" > taskman-secret.properties

echo "
db.host     = localhost
db.database = shipreq
db.username = $TASKMAN_DB_USERNAME
db.password = $TASKMAN_DB_PASSWORD
" > taskman-secret.properties
kubectl create secret generic taskman-secrets --from-file=secret.properties=taskman-secret.properties
