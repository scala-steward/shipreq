name := "backend_jobs"

version := "1.0.0-SNAPSHOT"

libraryDependencies ++= {
  val akkaVersion = "2.3.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "ch.qos.logback" % "logback-classic" % "1.1.1"
  )
}
