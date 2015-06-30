import sbt._
import Keys._
import org.sbtidea.SbtIdeaPlugin._

object IdeSettings {

  private object excludes {
    def common = List("project/target")
    def root   = common ++ List(".idea", ".idea_modules", ".settings", ".target", "log", ".bower")
    def webapp = common ++ List("vendor", "node_modules", "src/it/scala", "src/main/webapp/assets/vendor")
  }

  private def prefix(p: String)(ss: List[String]): List[String] = ss.map(p + _)
  private def prefixT(p: String)(ss: List[String]) = ss ++ prefix(p)(ss)

  def intellijSettingsForRoot = (p: Project) => p.settings(
    ideaProjectName := "ShipReq",
    ideaExcludeFolders := excludes.root ++ prefixT("webapp-server/")(excludes.webapp)
  )

  /*
  def eclipseSettings = (p: Project) => {
    import com.typesafe.sbteclipse.core.EclipsePlugin._
    p.settings(
      EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE17),
      EclipseKeys.withSource := true,
      EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
      // Prevent src/main/java appearing in .classpath
      unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(Seq(_)),
      // Prevent src/test/java appearing in .classpath
      unmanagedSourceDirectories in Test <<= (scalaSource in Test)(Seq(_))
      // This is a better way of doing it:
      unmanagedSourceDirectories in Compile ~= { _.filter(_.exists) }
      unmanagedSourceDirectories in Test ~= { _.filter(_.exists) }
    )
  }
  */
}
