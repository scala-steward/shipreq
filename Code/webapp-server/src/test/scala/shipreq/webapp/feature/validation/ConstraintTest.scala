package shipreq.webapp.feature.validation

import org.scalatest.{Matchers, FunSuite}
import shipreq.webapp.shared.AppConsts._
import shipreq.webapp.shared.validation._
import Constraint.{not => NOT}
import Constraints._

// TODO move into webapp-shared

class ConstraintTest extends FunSuite with Matchers {

  def test(i: String, expectPass: Boolean)(implicit c: Constraint[String]): Unit = {
    c.isValid(i) shouldBe expectPass
    c.invalidate(i).isEmpty shouldBe expectPass
  }
  def valid(i: String)(implicit c: Constraint[String]): Unit = test(i, true)
  def invalid(i: String)(implicit c: Constraint[String]): Unit = test(i, false)

  test("whitelistCharsS") {
    implicit val c = whitelistCharsS("a][b")("!")
    valid("")
    valid("[")
    valid("]")
    valid("aaa")
    valid("b[ba")
    valid("b[b]a")
    invalid("b[b]ac")
    invalid(" ")
    invalid("!")
  }

  test("whitelistCharsR") {
    implicit val c = whitelistCharsR("0-5")("!")
    valid("")
    valid("0")
    valid("321")
    invalid(" ")
    invalid("6")
    invalid("32156")
  }

  test("blacklistCharsS") {
    implicit val c = blacklistCharsS("a[b")("!")
    valid("")
    valid(" ")
    valid("hehehe!]")
    invalid("[")
    invalid("a")
    invalid("b")
    invalid("heheh[hehe")
  }

  test("blacklistCharsR") {
    implicit val c = blacklistCharsR("0-5")("!")
    valid("")
    valid(" ")
    valid("hehehe!]")
    invalid("3")
    invalid("heheh1hehe")
  }

  test("lengthInRange") {
    implicit val c = lengthInRange(2 to 4)
    invalid("")
    invalid("1")
    valid("12")
    valid("123")
    valid("1234")
    invalid("12345")
  }

  test("nonEmpty") {
    implicit val c = nonEmpty
    invalid("")
    valid("1")
    valid("12345")
  }

  test("largeTextLimit") {
    implicit val c = largeTextLimit
    valid("")
    valid("." * (largeTextMaxLength / 2))
    valid("." * largeTextMaxLength)
    invalid("." * (largeTextMaxLength + 1))
    invalid("." * (largeTextMaxLength * 2))
    c.invalidate("." * (largeTextMaxLength + 666)).head should include(" 666 ")
  }

  test("containsSurname") {
    implicit val c = containsSurname
    invalid("")
    invalid("a")
    invalid("abc")
    valid("abc abc")
    valid("B B")
    valid(" abc  abc ")
    valid(" abc  def qwe asdf")
    c.invalidate("").head shouldNot include("name")
    c.invalidate("firstOnly").head should include("name")
  }

  test("not(matchesR)") {
    implicit val c = NOT(matchesR("[0-9]+".r))("good")
    invalid("123")
    valid("yay")
  }
}
