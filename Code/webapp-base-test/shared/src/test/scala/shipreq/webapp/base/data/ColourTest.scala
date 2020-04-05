package shipreq.webapp.base.data

import shipreq.base.test.BaseTestUtil._
import utest._

object ColourTest extends TestSuite {

  override def tests = Tests {
    "contrast" - {

      "white" - {
        val c = Colour.white
        assertEqWithTolerance(c.contrastRatio(Colour.black), 21)
        assertEqWithTolerance(c.contrastRatio(Colour.white), 1)
        assertEq(c.foreground, Colour.black)
      }

      "black" - {
        val c = Colour.black
        assertEqWithTolerance(c.contrastRatio(Colour.black), 1)
        assertEqWithTolerance(c.contrastRatio(Colour.white), 21)
        assertEq(c.foreground, Colour.white)
      }

      "red" - {
        val c = Colour("#f00").get
        assertEq(c.foreground, Colour.white)
      }

      "orange" - {
        val c = Colour("#f80").get
        assertEqWithTolerance(c.contrastRatio(Colour.black), 8.77)
        assertEqWithTolerance(c.contrastRatio(Colour.white), 2.39)
        assertEq(c.foreground, Colour.white)
      }

      "yellow" - {
        val c = Colour("#ff0").get
        assertEq(c.foreground, Colour.black)
      }

      "blue" - {
        val c = Colour("#283ba3").get
        assertEqWithTolerance(c.contrastRatio(Colour.black), 2.24)
        assertEqWithTolerance(c.contrastRatio(Colour.white), 9.36)
        assertEq(c.foreground, Colour.white)
      }

    }
  }
}
