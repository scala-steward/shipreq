import sbt._
import Keys._
import org.scalajs.core.tools.io.{IO => _, _}
import org.scalajs.sbtplugin.ScalaJSPlugin
import Common.Functions._
import Common.Values.releaseMode
import Dependencies._
import DependencyLib.JVM
import ScalaJSPlugin.autoImport.{crossProject => _, _}
import ShipReq._

import com.earldouglas.xwp._
import ContainerPlugin.autoImport._
import JettyPlugin    .autoImport._
import WebappPlugin   .autoImport._

object WebappServer {
  val linkClientJs = taskKey[Unit]("Creates symlinks to webapp client resources.")
  val clientJsLinks = settingKey[ClientJsLinks]("Map of symlinks between client and server.")

  class ClientJsLinks(sRoot: File, tRoot: File) {
    private val s = sRoot / "scala-2.11"
    private val w = tRoot / "src/main/webapp"
    private def sPrefix = WebappClient.dir + "-"
    private def tName = "client.js"
    private val devMap = {
      val t = w / "dev"
      val js = s"${sPrefix}fastopt.js"
      val sourceMap = js + ".map"
      Map(
        s/js -> t/tName,
        s/sourceMap -> t/sourceMap) // Can't rename the sourcemap without changing it at the end of the JS
    }
    private val releaseMap =
      Map(s/s"${sPrefix}opt.js" -> w/"a"/tName)

    def links     = if (releaseMode) releaseMap else devMap
    def cleanable = (devMap.values.iterator ++ releaseMap.values).map(_.asFile).toSet[File]
  }

  lazy val jsBuildTask =
    WebappClient.jsTask in Compile in webappClient

  def clientJsSettings = (_: Project).settings(
    clientJsLinks := new ClientJsLinks((target in webappClient).value, baseDirectory.value),
    cleanFiles ++= clientJsLinks.value.cleanable.toSeq,
    { val k = Keys.`package`; k <<= k.dependsOn(linkClientJs) },
    { val k = webappPrepare ; k <<= k.dependsOn(linkClientJs) },
    // { val k = start in Jetty; k <<= k.dependsOn(linkClientJs) },
    // { val k = test in Test;   k <<= k.dependsOn(linkClientJs) },
    linkClientJs := {
      jsBuildTask.value // Ensure client JS is built
      val log = streams.value.log
      for ((s, t) <- clientJsLinks.value.links) {
        log.info(s"Copying $s → $t")
        IO.copyFile(s, t)
      }
    })

  def warSettings = {
    var dirHitList = Set("_scalate")
    if (releaseMode)
      dirHitList += "dev"

    (_: Project).settings(

      // Expand this the webapp-server module instead of building a jar
      // At the minimum, scripts in Release/webapp expect to find WEB-INF/classes/build.properties
      webappWebInfClasses := true,

      // Remove dirs from the WAR
      webappPostProcess := { webappDir =>
        def go(f: File): Unit = {
          if (f.isDirectory) {
            if (dirHitList contains f.getName) {
              streams.value.log.info(s"Deleting ${f.getAbsolutePath}")
              IO.delete(f)
            } else
              f.listFiles foreach go
          }
        }
        go(webappDir)
      })
  }

  def testSettings = (_: Project)
    .dependsOn(webappBaseTestJvm % "test->compile")
    .settings(inConfig(Test)(Seq(
      fork                         := true,
      javaOptions                  += "-Drun.mode=test",
      unmanagedResourceDirectories += baseDirectory.value / "src/main/webapp", // So templates load
      parallelExecution            := false)
    ): _*)

  def consoleCmds = """
      import scalaz._, shipreq.base.util._, shipreq.webapp._, db._, lib.Types._, feature.uc, uc._, uc.field._, uc.step._, uc.text._, FreeTextTerms._, util._
      def initlift() = {val b = new bootstrap.liftweb.Boot; b.configureLift; b}
                    """

  def apply = (_: Project)
    .enablePlugins(JettyPlugin, WarPlugin)
    .dependsOn(baseDb, taskmanApi, webappBaseJvm, webappBaseServerJvm)
    .deps(
      Scalaz.core ++ Lift.webkit ++ Shiro.all ++ scalate ++ commonsLang ++ guava ++
      testScope(μTest ++ scalaTest ++ scalaCheck ++ mockito ++ Lift.testkit ++ commonsIo ++ twitterEval) ++
      (LibJetty.webapp % "test") ++
      (LibJetty.servletApi % "test,provided"))
    .configure(
      webappSettings,
      Common.jvmSettings,
      Common.generateBuildPropFile(),
      clientJsSettings,
      warSettings,
      testSettings,
      dontInline) // crashes scalac 2.11.7
    .settings(
      addCommandAlias("livejs", "~;clear;jsp"),
      containerLibs in Jetty := LibJetty.runner(JVM).map(_.intransitive()),
      javaOptions in Jetty += "-Xmx1g",
      initialCommands += consoleCmds,
      fullClasspath in console in Compile += file("src/main/webapp")) // So templates can be loaded from console
}
