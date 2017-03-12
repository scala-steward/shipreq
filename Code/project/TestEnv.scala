import sbt._, Keys._

object TestEnv {

  val testEnvStart = taskKey[Unit]("Starts up a test environment.")

  val startOnce: () => Unit = {
    var up = false
    () =>
      if (!up) {
        import sys.process._
        "bin/env test up -d".!! // throws
        up = true
      }
  }

  val required: Project => Project =
    _.settings(
      testEnvStart in ThisBuild := startOnce(),
      testOptions in Test += Tests.Setup(startOnce))
}