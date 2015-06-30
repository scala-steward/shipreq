package shipreq.base.util

import utest._

object BiMapTest extends TestSuite {
  override def tests = TestSuite {
    "Adding & retrieving" - {
      val b = new BiMapBuilder[String,Int]
      b += ("Three" -> 3)
      b("Two") = 2
      val m = b.result
      assert(m.ba(3) == "Three")
      assert(m.ab("Two") == 2)
    }
  }
}
