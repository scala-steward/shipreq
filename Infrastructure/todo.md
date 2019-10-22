* Bastion
  * Stop using a shared key - key should be per user
  * Use AWS Session Manager instead of opening an SSH port
  * Collect logs
  * Healthcheck & recovery

* Portal
  * logs

* NAT
  * Collect logs
  * Healthcheck & recovery

* Ops EC2
  * Collect logs

* Prometheus
  * Healthcheck & recovery
  * custom image
  * custom image CI
  * logs
  * configure retention period & storage onto EBS


====================================================================================================

* For each new resource/service consider:
  * Healthcheck & recovery
  * custom image
  * custom image CI
  * logs
  * EBS

* RDS

* RDS initialisation
  Seems the only way is RDS event subscription + SNS + lambda

* FileBeat
* EC2 metrics; node_exporter?

* App ALB
* ShipReq
* Taskman

* Grafana
* Storage