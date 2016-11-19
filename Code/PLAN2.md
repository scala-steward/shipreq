Goals
=====
* Build
  * Build docker images for taskman & webapp
  * Publish docker images (?)
* Test
  * Setup env via docker
* Dev run
  * Setup env via docker
  * Run apps via SBT (which points to docker env)
* Local run
  * docker compose starts up everything
* Deploy
  * TODO: ansible & docker?

Concerns
========
* config management
  * suggestion: use env vars only, don't have runModes
  * idea: config service
* admin tasks / one-off app runs (requires solution to config)
* docker image repo
* jetty upgrade procedure

Tasks
=====
* new config machinery (?)
  * provinence per key
  * warn unused keys
  * error missing keys
  * print all config on startup

* Test env
  * Create docker compose setup for external resources
  * Update tests to use test-env

* Taskman ⇒ Docker
  * Ensure enough build info in jar (and dockerfile)
  * Restructure non-test config
  * Log test to file, non-test to stdout
  * Copy scripts and resources
  * Build docker

* ShipReq ⇒ Docker
  * Ensure enough build info in jar (and dockerfile)
  * Restructure non-test config
  * Log test to file, non-test to stdout
  * Copy scripts and resources
  * Port: `war-compress_static_resources`
  * Port: `war-force_https`
  * Build docker

* Dev env
  * Create docker compose setup for external resources
  * Configure run in SBT to use ↑

* Local env
  * Create docker compose setup for everything
  * Make DB persisent

* Update release scripts in Code/bin/

