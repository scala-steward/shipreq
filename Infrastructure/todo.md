* Bastion
  * Stop using a shared key - key should be per user
  * Use AWS Session Manager instead of opening an SSH port
  * Collect logs
  * Healthcheck & recovery

* Portal
  * custom image
  * custom image CI
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
  * EBS



====================================================================================================

* For each new resource/service consider:
  * Healthcheck & recovery
  * custom image
  * custom image CI
  * logs
  * EBS
