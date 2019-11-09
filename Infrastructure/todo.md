* Bastion
  * Stop using a shared key - key should be per user
  * Use AWS Session Manager instead of opening an SSH port
  * Log logins
  * Log commands (?)
  * Log portal (?)
  * node_exporter (?)
  * cadvisor (?)
  * filebeat (?)

* NAT
  * Collect logs / filebeat (?)
  * node_exporter (?)

* Cluster EC2s
  * Collect logs (?)

====================================================================================================

* App
  * ALB
  * ShipReq
  * Taskman
  * filebeat
  * cadvisor
  * node_exporter

* Ops
  * postgres_exporter

* Alerting

* DR
  * Postgres
  * EBS
