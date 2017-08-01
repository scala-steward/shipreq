provider "google" {
  credentials = "${file(".terraform-key.json")}"
  project     = "shipreq-dev"
  region      = "australia-southeast1"
}

resource "google_compute_instance" "taskman" {
  name         = "taskman"
  machine_type = "f1-micro"
  zone         = "australia-southeast1-c"

  disk {
    image       = "cos-cloud/cos-stable"
    size        = 10
    auto_delete = true
    boot        = true
  }

  scheduling {
    preemptible       = true
    automatic_restart = true
  }

  network_interface {
    network = "default"
  }

  service_account {
    scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring.write",
      "https://www.googleapis.com/auth/trace.append",
    ]
  }
}
