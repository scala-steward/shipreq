package shipreq.base.util

import japgolly.microlibs.testutil.TestUtil._
import nyaya.gen.Gen
import scalaz.{-\/, \/-}
import utest._

object UtilTest extends TestSuite {

  override def tests = Tests {
    "partitionConsecutive" - {
      def test(in: Int*)(a: Int*)(b: Int*) =
        assertEq(Util.partitionConsecutive(in.toList), (a.toList, b.toList))
      * - test()()()
      * - test(3)(3)()
      * - test(3, 4)(3, 4)()
      * - test(3, 5)(3)(5)
      * - test(3, 5, 6)(3)(5, 6)
      * - test(3, 4, 6)(3, 4)(6)
      * - test(3, 4, 5, 6)(3, 4, 5, 6)()
    }

    "separateByWhitespaceOrCommas" - {

      "manual" - assertEq(
        Util.separateByWhitespaceOrCommas("omg  , k qq"),
        Vector(\/-("omg"), -\/("  , "), \/-("k"), -\/(" "), \/-("qq")))

      "prop" - {
        val gen = Gen.chooseChar(' ', ",ab")
        for {
          n <- 0 to 6
          s <- gen.string(n).samples().take(Math.pow(n, 2).toInt + 1)
        } {
          val r = Util.separateByWhitespaceOrCommas(s)
          assertEq(r.iterator.map(_.merge).mkString, s)
        }
      }
    }
  }
}
