First-Time Procedure
====================

###  Local setup

* Install Taskman JAR.
    Build locally first.
    ./install-jar

* Determine IP for commands below
    export ip=$(../util/ip-shipreq)

### Deployment

    ./deploy-taskman $ip
    ./deploy-jar $ip


Upgrade Procedure
=================

    ./install-war
    ./deploy-taskman $ip   # If needed
    ./deploy-jar $ip
    # TODO: ssh $(<deployment-user)@$ip taskman/bin/restart

