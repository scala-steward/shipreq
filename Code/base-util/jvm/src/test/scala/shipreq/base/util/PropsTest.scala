package shipreq.base.util

import japgolly.clearconfig._
import scalaz.-\/
import scalaz.syntax.applicative._
import shipreq.base.util.FxModule._
import utest._

object PropsTest extends TestSuite {

  override def tests = Tests {

    'inlineProps - {

      'ok - {
        val src1 = ConfigSource.manual[Fx]("a")(
          "x.1" -> "hehe",
          Props.InlineProperties.key.value ->
            s"""
               |# hehe
               | x.2      = 123
               | x.3    = good stuff # nice
               |""".stripMargin
        )
        val src = Props.InlineProperties(src1)

        val cfgDef = (
          ConfigDef.need[String]("x.1") |@|
          ConfigDef.need[String]("x.2") |@|
          ConfigDef.need[String]("x.3")
          ) ((_, _, _)).withReport

        val (xs, report) = cfgDef.run(src).unsafeRun().getOrDie()
        assert(xs == (("hehe", "123", "good stuff")))
        assert(!report.full.contains(Props.InlineProperties.key.value))
        report.full
      }

      'ko - {
        val src1 = ConfigSource.manual[Fx]("a")(
          "x.1" -> "hehe",
          Props.InlineProperties.key.value ->
            s"""
               |# hehe
               | x.1      = 123
               |""".stripMargin
        )
        val src = Props.InlineProperties(src1)

        val cfgDef = ConfigDef.need[String]("x.1")

        val result = cfgDef.run(src).unsafeRun().toDisjunction
        assert(result == -\/("Error preparing source [SourceName(a)]: The following keys are defined at both the top-level and in SHIPREQ_INLINE_PROPERTIES: x.1."))
      }

    }

  }
}
