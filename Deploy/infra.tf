variable "GCP_PROJECT" {}
variable "GCP_REGION" {}
variable "GCP_ZONE" {}
variable "CLUSTER_NAME" {}
variable "DB_INSTANCE" {}

provider "google" {
  project = "${var.GCP_PROJECT}"
  region  = "${var.GCP_REGION}"
}

################################################################################
# Terraform state
################################################################################

terraform {
  backend "gcs" {
    path = "terraform.tfstate"
  }
}

################################################################################
# DB
################################################################################

resource "google_sql_database_instance" "master" {
  name                     = "${var.DB_INSTANCE}"
  region                   = "${var.GCP_REGION}"
  database_version         = "POSTGRES_9_6"
  settings {
    tier                   = "db-f1-micro"
    disk_size              = 10
    activation_policy      = "ALWAYS"
    location_preference {
      zone                 = "${var.GCP_ZONE}"
    }
    backup_configuration {
      enabled              = true
      start_time           = "16:00"
    }
    maintenance_window {
      day                  = 6
      hour                 = 16
    }
  }
}

resource "google_sql_database" "shipreq" {
  name      = "shipreq"
  instance  = "${google_sql_database_instance.master.name}"
  charset   = "UTF8"
  collation = "en_US.UTF8" # https://github.com/terraform-providers/terraform-provider-google/pull/229
}

################################################################################
# Cluster
################################################################################

resource "google_container_cluster" "apps" {
  name               = "${var.CLUSTER_NAME}"
  zone               = "${var.GCP_ZONE}"
  initial_node_count = 1
  node_config {
    disk_size_gb     = 10
    machine_type     = "custom-2-2560"
    image_type       = "COS"
    oauth_scopes     = [
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/monitoring.write",
      "https://www.googleapis.com/auth/service.management.readonly",
      "https://www.googleapis.com/auth/servicecontrol",
      "https://www.googleapis.com/auth/trace.append",
    ]
  }
  addons_config {
    http_load_balancing { disabled = true }
    horizontal_pod_autoscaling { disabled = true }
  }
}

################################################################################
# Taskman account
################################################################################

resource "google_service_account" "taskman" {
  account_id   = "taskman"
  display_name = "Taskman service account"
}

resource "google_project_iam_policy" "taskman" {
  project     = "${var.GCP_PROJECT}"
  policy_data = "${data.google_iam_policy.taskman.policy_data}"
}

data "google_iam_policy" "taskman" {
  binding {
    role = "roles/cloudsql.client"
    members = [
      "serviceAccount:${google_service_account.taskman.email}",
    ]
  }
}

################################################################################
# Webapp account
################################################################################

resource "google_service_account" "webapp" {
  account_id   = "webapp"
  display_name = "Webapp service account"
}

resource "google_project_iam_policy" "webapp" {
  project     = "${var.GCP_PROJECT}"
  policy_data = "${data.google_iam_policy.webapp.policy_data}"
}

data "google_iam_policy" "webapp" {
  binding {
    role = "roles/cloudsql.client"
    members = [
      "serviceAccount:${google_service_account.webapp.email}",
    ]
  }
}
