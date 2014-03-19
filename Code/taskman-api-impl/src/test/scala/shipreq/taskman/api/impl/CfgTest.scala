package shipreq.taskman.api.impl

import org.specs2.mutable.Specification
import scalaz.Free.FreeC
import shipreq.base.test.db.specs2.DatabaseTest
import shipreq.taskman.FreeEffect._
import shipreq.taskman.api.ApiOp
import ApiOp.CfgPut
import TaskmanApiImpl._

class CfgTest extends Specification with DatabaseTest {

  def run[A](cmds: FreeC[ApiOp, A]): A =
    compile(cmds, reify(new GlobalContext(None), session)).unsafePerformIO()

  "CfgPut" should {

    "insert new" in {
      run(CfgPut("a", "start") >> CfgPut("b", "omg"))
      sql"select k,v from cfg where k in ('a','b')".as[(String, String)].list ==== List(("a", "start"), ("b", "omg"))
    }

    "update existing" in {
      run(CfgPut("a", "start") >> CfgPut("b", "omg") >> CfgPut("a", "heheh"))
      sql"select k,v from cfg where k in ('a','b')".as[(String, String)].list ==== List(("a", "heheh"), ("b", "omg"))
    }
  }

}