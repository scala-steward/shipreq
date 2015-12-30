package shipreq.webapp.server.feature.validation

import org.scalatest.{Matchers, FunSpec}
import scala.xml.Text
import scalaz.old.NonEmptyList
import scalaz.syntax.semigroup._
import shipreq.webapp.base.validation._
import VFailure.semigroup

class VFailureRendererTest extends FunSpec with Matchers {

  val singleField = VFailure.forField("Car", NonEmptyList("is too big."))
  val multiField = VFailure.forField("Car", NonEmptyList("is too fast.", "is too big."))
  val singleLoose = VFailure.looseMsg("It's Tuesday.")
  val multiLoose = singleLoose |+| VFailure.looseMsg("It's too hot.")
  val multiTypes = singleLoose |+| singleField
  val multiTypes4 = multiLoose |+| multiField

  describe("Rendering to html") {
    it("Single field error") {
      VFailureHtmlRenderer render singleField shouldBe Text("Car is too big.")
    }
    it("Multiple field errors") {
      VFailureHtmlRenderer render multiField shouldBe Text("Car") ++ <ul><li>is too fast.</li><li>is too big.</li></ul>
    }
    it("Single loose error") {
      VFailureHtmlRenderer render singleLoose shouldBe Text("It's Tuesday.")
    }
    it("Multiple loose error") {
      VFailureHtmlRenderer render multiLoose shouldBe <ul><li>It's too hot.</li><li>It's Tuesday.</li></ul>
    }
    it("Different error types 1") {
      VFailureHtmlRenderer render multiTypes shouldBe <ul><li>It's Tuesday.</li><li>Car is too big.</li></ul>
    }
    it("Different error types 2") {
      VFailureHtmlRenderer render multiTypes4 shouldBe <ul><li>It's too hot.</li><li>It's Tuesday.</li><li>Car<ul><li>is too fast.</li><li>is too big.</li></ul></li></ul>
    }
  }

}
