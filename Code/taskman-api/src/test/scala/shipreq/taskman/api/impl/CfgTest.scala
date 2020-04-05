package shipreq.taskman.api.impl

import doobie.imports._

import utest._

object CfgTest extends TestSuite with ApiImplTestHelpers {

  private lazy val q = Query0[(String, String)]("select k,v from cfg where k in ('a','b') order by 1")

  override def tests = Tests {

    "cfgPut" - {

      "insert" - {
        val result = run(xa =>
          for {
            _ <- xa.cfgPut("a", "start")
            _ <- xa.cfgPut("b", "omg")
            r <- q.list.transact(xa)
          } yield r
        )
        assert(result == List(("a", "start"), ("b", "omg")))
      }

      "update" - {
        val result = run(xa =>
          for {
            _ <- xa.cfgPut("a", "start")
            _ <- xa.cfgPut("b", "omg")
            _ <- xa.cfgPut("a", "heheh")
            r <- q.list.transact(xa)
          } yield r
        )
        assert(result == List(("a", "heheh"), ("b", "omg")))
      }
    }

  }
}
