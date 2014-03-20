package shipreq.base.util

import java.util.{Properties, Locale}
import scalaz.\/-
import shipreq.base.util.ExternalValueReader.Retriever

object RunMode extends Enumeration {

  val Development = Value(1, "Development")
  val Test        = Value(2, "Test")
  val Staging     = Value(3, "Staging")
  val Production  = Value(4, "Production")
  val Pilot       = Value(5, "Pilot")
  val Profile     = Value(6, "Profile")

  def namesFor(m: Value): List[String] = m match {
    case Development => List("dev", "development")
    case Test        => List("test")
    case Staging     => List("staging")
    case Production  => List("prod", "production")
    case Pilot       => List("pilot")
    case Profile     => List("profile")
  }

  private[this] val normaliseName: String => String = _ toLowerCase Locale.ENGLISH

  private[this] val nameToMode: Map[String, Value] =
      values.toList.flatMap(m =>
        (m.toString :: namesFor(m))
          .map(n => (normaliseName(n) -> m))
      ).toMap

  def forName(n: String): Option[Value] =
    nameToMode.get(normaliseName(n))

  def retriever(implicit r: Retriever[String]): Retriever[Value] =
    new StringBasedValueReader(r).tryParseE[Value](s =>
      forName(s) match {
        case Some(m) => \/-(m)
        case None    => Error(s"Unable to parse run mode: $s")
      }
    )

  val retrieverFromSysProps: Retriever[Value] =
    retriever(JPropertiesValueReader(Props.systemProps(new Properties)).retrieverS)

  def detectFromStackTrace(st: Array[StackTraceElement] = Thread.currentThread.getStackTrace): Value =
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
