start/stop
env vars


### Test Taskman/ShipReq
* Needs DB
* run `test`
* Have script or docker-compose run externally. Maybe have SBT autostart.
  * docker-compose with test env vars
  * feed env vars to process via SBT
* OR use postgres

### Dev ShipReq
* Needs DB
* Maybe Taskman ??????????
* run `up`

### Prod
* aws via ansible
* Publish & run docker images

### Local Prod
* Needs DB
* Run docker images

================================================================================

How to share config?
How to manage Jetty & upgrades?

================================================================================

SBT should build docker images
* Move all release bin, logic, resources into src/release/?

================================================================================

* Remove all config from */src/main/resources
* Unit testing
  * Put all test config in */src/test/resources
  * Create docker-compose for test DB, use in test config files
* Dev Taskman
  * Create docker-compose for dev DB
  * Store config in src/dev/resources and provide via SBT
* Dev webapp
  * Create docker-compose for dev DB
  * Store config in src/dev/resources and provide via SBT
  * (doesn't need taskman as msgs will just remain unread)

* Release
  * Builds docker images (containing no config)
  * Taskman
    * copy non-config (like bin/) from Release to src/release
  * webapp
    * copy non-config (like bin/) from Release to src/release
    * builds on Jetty
    * move all pre-processing steps into SBT
* Local prod
  * Lives outside of Code
  * Create docker-compose for DB, taskan, webapp, elk, grafana etc.
  * Settings stored in files and provided via compose config/script (through volume mounting)
* Prod
  * Setup infra using ansible
  * ec2 task defs for DB, taskan, webapp, elk, grafana etc.
  * Settings stored in files (encrypted) and provided via ansible (somehow)

