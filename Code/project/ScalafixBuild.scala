import sbt._
import sbt.Keys._
import scalafix.sbt.BuildInfo.{scalafixVersion => ScalafixVer}
import scalafix.sbt.ScalafixPlugin

object ScalafixBuild {

  private val settings: Project => Project =
    Common.settingsMinForScalafix.andThen(
      _.settings(scalacOptions ~= { _.filterNot(_ startsWith "-Yimports") })
    )

  lazy val `scalafix-input` = (project in file("scalafix/input"))
    .configure(settings)
    .disablePlugins(ScalafixPlugin)

  lazy val `scalafix-output` = (project in file("scalafix/output"))
    .configure(settings)
    .disablePlugins(ScalafixPlugin)

  lazy val `scalafix-rules` = (project in file("scalafix/rules"))
    .configure(settings)
    .disablePlugins(ScalafixPlugin)
    .settings(libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % ScalafixVer)

  // Pending: https://github.com/scalacenter/scalafix/issues/1230
  //lazy val `scalafix-tests` = (project in file("scalafix/tests"))
  //  .configure(settings)
  //  .settings(
  //    libraryDependencies                    += "ch.epfl.scala" % "scalafix-testkit" % ScalafixVer % Test cross CrossVersion.full,
  //    scalafixTestkitOutputSourceDirectories := sourceDirectories.in(`scalafix-output`, Compile).value,
  //    scalafixTestkitInputSourceDirectories  := sourceDirectories.in(`scalafix-input`, Compile).value,
  //    scalafixTestkitInputClasspath          := fullClasspath.in(`scalafix-input`, Compile).value,
  //    scalafixTestkitInputScalacOptions      := scalacOptions.in(`scalafix-input`, Compile).value,
  //    scalafixTestkitInputScalaVersion       := scalaVersion.in(`scalafix-input`, Compile).value
  //  )
  //  .dependsOn(`scalafix-input`, `scalafix-rules`)
  //  .enablePlugins(ScalafixTestkitPlugin)

}
