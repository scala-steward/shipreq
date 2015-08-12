Development Environment Setup
=============================

### Scala

* Install SBT.

### Webapp

* yaourt -S --needed --noconfirm nodejs-{bower,grunt-cli}
* npm install
* bower list
* cd webapp-server && grunt

### Database

* Install PostgreSQL
    sudo pacman -Sy --needed postgresql

    sudo mkdir /var/lib/postgres/data
    sudo chown -c -R postgres:postgres /var/lib/postgres
    sudo -u postgres initdb -D /var/lib/postgres/data

    sudo systemctl enable postgresql
    sudo systemctl start postgresql

    sudo -u postgres cp /var/lib/postgres/data/pg_hba.conf{,.orig}
    sudo -u postgres vim /var/lib/postgres/data/pg_hba.conf
    Add before any other rules:
        local all postgres ident
    Change trust to md5 where appropriate

    sudo systemctl restart postgresql

* Create local DBs
    bin/db/create dev
    bin/db/create test

* Initialise
  1. Setup the taskman schema.
     Required so that webapp can startup and issue `CfgPut`s.
     `bin/taskman/db-migrate`
  2. Start the webapp.
     Issues `CfgPuts` required by Taskman on startup.
     `bin/webapp/run`

### Dev account for web front-end

1. `bin/run`
2. Open http://localhost:8080/register
3. Create an account with
    Email:    japgolly@gmail.com
    Username: devuser
    Password: dev123123
4. Auto login at http://localhost:8080/x (dev-mode only)


If Taskman runs into trouble, tickets should be raised at http://yoarmum.freshdesk.com/

You can also inspect the message queue via:
1. `bin/db/connect dev`
2. `set search_path TO taskman`
3. `select * from msgq;`

Also useful: `update msgq set effective_from = now();`



Updating Dependencies
=====================

### Scala

Dependencies and their versions are declared in `project/Dependencies.scala`.
Dependencies are assigned to modules in `project/Build.scala`.
The version of SBT itself is configured in `project/build.properties`.

### Webapp

* Updating
  1. bower list
  2. bower update
  3. cd webapp-server/
  4. grunt

* Adding/Removing
  1. Edit bower.json
  2. bower install
  3. cd webapp-server/
  4. Edit Gruntfile.js
  5. grunt

### Jetty

Follow instructions in `../Release/webapp/README.md`.


Development
===========

### Running

Everything: `bin/run`
Taskman:    `bin/taskman/run`
Webapp:     `bin/webapp/run`

### Continuous Building

`run/dev`

### Testing

Scala: `sbt test`

Javascript:
  1. Either load `webapp/src/test/javascript/tests.html` in a browser.
  2. Or `cd webapp && grunt test`.


Releasing
=========
[TODO](Release instructions are out-of-date)

1. ./release
2. cd ../../Release
3. ./install-latest_war
4. [OPTIONAL] cd webapp && ./jetty and test locally.
5. ./deploy-war <ip>

