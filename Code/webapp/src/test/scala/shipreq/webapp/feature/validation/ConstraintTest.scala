package shipreq.webapp.feature.validation

import org.scalatest.{Matchers, FunSuite}
import shipreq.webapp.app.AppConfig._
import Constraint.{not => NOT}
import Constraints._

class ConstraintTest extends FunSuite with Matchers {

  def test(i: String, expectPass: Boolean)(implicit c: Constraint[String]): Unit = {
    c.isValid(i) shouldBe expectPass
    c.invalidate(i).isEmpty shouldBe expectPass
  }
  def pass(i: String)(implicit c: Constraint[String]): Unit = test(i, true)
  def fail(i: String)(implicit c: Constraint[String]): Unit = test(i, false)

  test("whitelistCharsS") {
    implicit val c = whitelistCharsS("a[b")("!")
    pass("")
    pass("[")
    pass("aaa")
    pass("b[ba")
    fail("b[b]a")
    fail(" ")
    fail("!")
  }

  test("whitelistCharsR") {
    implicit val c = whitelistCharsR("0-5")("!")
    pass("")
    pass("0")
    pass("321")
    fail(" ")
    fail("6")
    fail("32156")
  }

  test("blacklistCharsS") {
    implicit val c = blacklistCharsS("a[b")("!")
    pass("")
    pass(" ")
    pass("hehehe!]")
    fail("[")
    fail("a")
    fail("b")
    fail("heheh[hehe")
  }

  test("blacklistCharsR") {
    implicit val c = blacklistCharsR("0-5")("!")
    pass("")
    pass(" ")
    pass("hehehe!]")
    fail("3")
    fail("heheh1hehe")
  }

  test("lengthInRange") {
    implicit val c = lengthInRange(2 to 4)
    fail("")
    fail("1")
    pass("12")
    pass("123")
    pass("1234")
    fail("12345")
  }

  test("nonEmpty") {
    implicit val c = nonEmpty
    fail("")
    pass("1")
    pass("12345")
  }

  test("largeTextLimit") {
    implicit val c = largeTextLimit
    pass("")
    pass("." * (LargeTextMaxLength / 2))
    pass("." * LargeTextMaxLength)
    fail("." * (LargeTextMaxLength + 1))
    fail("." * (LargeTextMaxLength * 2))
    c.invalidate("." * (LargeTextMaxLength + 666)).head should include(" 666 ")
  }

  test("containsSurname") {
    implicit val c = containsSurname
    fail("")
    fail("a")
    fail("abc")
    pass("abc abc")
    pass("B B")
    pass(" abc  abc ")
    pass(" abc  def qwe asdf")
    c.invalidate("").head shouldNot include("name")
    c.invalidate("firstOnly").head should include("name")
  }

  test("not(matchesR)") {
    implicit val c = NOT(matchesR("[0-9]+".r))("good")
    fail("123")
    pass("yay")
  }
}
