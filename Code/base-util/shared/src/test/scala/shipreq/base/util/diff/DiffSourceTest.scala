package shipreq.base.util.diff

import japgolly.microlibs.testutil.TestUtil._
import sourcecode.Line
import utest._

object DiffSourceTest extends TestSuite {

  private def assertSubtr(a: DiffSource[String, Any])(offset: Int, expected: String)(implicit l: Line): Unit =
    assertEq(a.offset -> a.value, offset -> expected)

  override def tests = Tests {
    "lines" - {

      "1" - {
        val str = "\n\n12345678\n\nabcdefgh\n\n"
        val s   = DiffSource.Str.lines(str)

        "full" - assertSubtr(s.value)(0, str)
        "0"    - assertSubtr(s.just(0).value)(0, "\n\n12345678")
        "1"    - assertSubtr(s.just(1).value)(10, "\n\nabcdefgh\n\n")
        "L0"   - assertEq(s(0).value, "12345678")
        "L1"   - assertEq(s(1).value, "abcdefgh")
      }

      "2" - {
        val str = "a\nb\nc\nd"
        val s   = DiffSource.Str.lines(str)

        "full" - assertSubtr(s.value)(0, str)
        "0"    - assertSubtr(s.slice(0, 1).value)(0, "a")
        "1"    - assertSubtr(s.slice(1, 2).value)(1, "\nb")
        "2"    - assertSubtr(s.slice(2, 3).value)(3, "\nc")
        "3"    - assertSubtr(s.slice(3, 4).value)(5, "\nd")
        "01"   - assertSubtr(s.slice(0, 2).value)(0, "a\nb")
        "12"   - assertSubtr(s.slice(1, 3).value)(1, "\nb\nc")
        "23"   - assertSubtr(s.slice(2, 4).value)(3, "\nc\nd")
        "012"  - assertSubtr(s.slice(0, 3).value)(0, "a\nb\nc")
        "123"  - assertSubtr(s.slice(1, 4).value)(1, "\nb\nc\nd")
      }

      "3" - {
        //                            012345 678901234 567890 123 456789
        //                            |    |         |      |   |
        val s = DiffSource.Str.lines("BADBC\nDACAABDD\nBCDDB\nDA\nDCACC")
        assertEq(s.length, 5)

        assertSubtr(s(0))(0, "BADBC")
        assertSubtr(s(1))(6, "DACAABDD")
        assertSubtr(s(2))(15, "BCDDB")
        assertSubtr(s(3))(21, "DA")
        assertSubtr(s(4))(24, "DCACC")

        assertSubtr(s.just(0).value)(0, "BADBC")
        assertSubtr(s.just(1).value)(5, "\nDACAABDD")
        assertSubtr(s.just(2).value)(14, "\nBCDDB")
        assertSubtr(s.just(3).value)(20, "\nDA")
        assertSubtr(s.just(4).value)(23, "\nDCACC")
      }

    }
  }
}
