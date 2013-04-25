* Start WebServer
    sbt container:start

* Stop WebServer
    sbt container:stop

* Continuous Redeploy
    sbt
    ~; container:start; container:reload /
