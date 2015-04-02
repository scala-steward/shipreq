package shipreq.webapp.base.test

import shipreq.base.util.ScalaExt._
import scalaz.Equal
import scalaz.syntax.equal._

object BaseTestUtil extends BaseTestUtil

trait BaseTestUtil {

  def assertEq[A: Equal](actual: A, expect: A): Unit =
    assertEq(None, actual, expect)

  def assertEq[A: Equal](name: String, actual: A, expect: A): Unit =
    assertEq(name.some, actual, expect)

  def assertEq[A: Equal](name: Option[String], actual: A, expect: A): Unit =
    if (actual ≠ expect) {
      println()
      name.foreach(n => println(s">>>>>>> $n"))
      val as = actual.toString
      val es = expect.toString
      if ((as + es) contains "\n")
        println(s"actual: ↙[\n$as]\nexpect: ↙[\n$es]")
      else
        println(s"actual: [$as]\nexpect: [$es]")
      println()
      assert(false)
    }

  def assertSet[A](actual: Set[A])(expect: A*): Unit = {
    val e = expect.toSet
    val missing = e -- actual
    val unexpected = actual -- e
    if (missing.nonEmpty || unexpected.nonEmpty)
      fail(s"Actual: $actual\nExpect: $e\n   Missing: $missing\nUnexpected: $unexpected")
  }

  def fail(msg: String): Nothing =
    throw new AssertionError(msg)
}