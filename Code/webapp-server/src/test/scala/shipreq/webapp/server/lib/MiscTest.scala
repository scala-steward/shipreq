package shipreq.webapp.server.lib

import org.scalatest.FunSpec
import org.scalatest.Matchers
import net.liftweb.util.Helpers._
import scalaz.Cord

class MiscTest extends FunSpec with Matchers with Misc {

  describe("filterCovar()") {} // TODO

  describe("#randomConfirmationToken") {
    it("should return different values each time") {
      randomConfirmationToken should not be(randomConfirmationToken)
      randomConfirmationToken should not be(randomConfirmationToken)
      randomConfirmationToken should not be(randomConfirmationToken)
    }
  }

  describe("Cord.isEmpty") {
    it("should return true for Cord.empty") {
      Cord.empty.isEmpty should be(true)
    }
    it("should return true when multiple empty cords appended") {
      (Cord("") ++ Cord("") :+ "").isEmpty should be(true)
    }
    it("should return false when not empty") {
      (Cord("") ++ Cord("") :+ "a").isEmpty should be(false)
      (Cord("") ++ Cord("a") :+ "").isEmpty should be(false)
      (Cord("a") ++ Cord("") :+ "").isEmpty should be(false)
    }
  }
}
