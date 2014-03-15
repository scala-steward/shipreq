package shipreq.base.util

object RunMode extends Enumeration {

  val Development = Value(1, "Development")
  val Test        = Value(2, "Test")
  val Staging     = Value(3, "Staging")
  val Production  = Value(4, "Production")
  val Pilot       = Value(5, "Pilot")
  val Profile     = Value(6, "Profile")

  def names(m: Value): List[String] = m match {
    case Development => List("dev", "development")
    case Test        => List("test")
    case Staging     => List("staging")
    case Production  => List("prod", "production")
    case Pilot       => List("pilot")
    case Profile     => List("profile")
  }

  def detect(st: Array[StackTraceElement] = Thread.currentThread.getStackTrace): Value =
    if (doesStackTraceContainKnownTestRunner(st))
      Test
    else
      Development

  private def doesStackTraceContainKnownTestRunner(st: Array[StackTraceElement]): Boolean = {
    val names = List(
      "org.apache.maven.surefire.booter.SurefireBooter",
      "sbt.TestRunner",
      "org.jetbrains.plugins.scala.testingSupport.scalaTest.ScalaTestRunner",
      "org.scalatest.",
      "org.scalatools.testing.",
      "org.specs2."
    )
    st.exists(e => names.exists(e.getClassName.startsWith))
  }
}
