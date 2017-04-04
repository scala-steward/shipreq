import sbt._, Keys._

object DockerEnv {
  import sys.process._

  def javaOptionsFromDockerComposeEnv(serviceName: String, dockerComposeYml: File): List[String] =
    javaOptionsFromDockerComposeEnv(serviceName, IO readLines dockerComposeYml)

  def javaOptionsFromDockerComposeEnv(serviceName: String, dockerComposeYml: List[String]): List[String] = {
    var inService = false
    var inEnv = false
    val b = List.newBuilder[String]
    val service = s"  $serviceName:"
    dockerComposeYml foreach {
      case `service`                                           => inService = true
      case s if s.matches("^  [a-z].*:")                       => inService = false; inEnv = false
      case "    environment:"                                  => inEnv = true
      case s if s.matches("^    [a-z].*:")                     => inEnv = false
      case s if inService && inEnv && s.startsWith("      - ") => b += "-D" + s.drop(8)
      case _                                                   => ()
    }
    b.result()
  }

  def javaOptionsFromProps(props: File): List[String] =
    javaOptionsFromProps(IO readLines props)

  def javaOptionsFromProps(props: List[String]): List[String] =
    props
      .iterator
      .map(_.replaceFirst(" *#.+", "").trim.replaceFirst(" *= *", "="))
      .filter(_.nonEmpty)
      .map("-D" + _)
      .toList

  class ServiceRef(startFn: () => Unit, stopFn: () => Unit) {
    private var up = false

    val start: () => Unit = () =>
      if (!up) {
        startFn()
        up = true
      }

    val stop: () => Unit = () => {
      up = false
      stopFn()
    }
  }

  def envRef(env: String, services: String*) = {
    val ss = services.mkString(" ")
    new ServiceRef(
      () => s"bin/env $env up -d $ss".!!,
      () => s"bin/env $env stop $ss".!!)
  }

  // ===================================================================================================================

  object dev {

    val devEnvStart = taskKey[Unit]("Starts up the dev environment.")
    val devEnvStop = taskKey[Unit]("Stops the dev environment.")

    private val env = envRef("dev", "postgres")

    val commands: Project => Project =
      _.settings(
        devEnvStart in ThisBuild := env.start(),
        devEnvStop in ThisBuild := env.stop())
  }

  // ===================================================================================================================

  object test {

    val testEnvStart = taskKey[Unit]("Starts up a test environment.")

    private val env = envRef("test")

    val required: Project => Project =
      _.settings(
        testEnvStart in ThisBuild := env.start(),
        testOptions in Test += Tests.Setup(env.start))
  }
}
