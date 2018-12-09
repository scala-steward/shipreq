package shipreq.taskman.server

import shipreq.base.test.db.SqlTester.test
import shipreq.taskman.server.ServerOpFx.Sql._
import utest._

object ServerOpSqlTest extends TestSuite {

  override def tests = Tests {

    //  "getNextNodeIdQ" - test(getNextNodeIdQ)
    "cfgGetQ" - test(cfgGetQ)
    //  "getMsgsAssignNodeZ" - test(getMsgsAssignNodeZ)
    //  "getMsgsAssignNodeF" - test(getMsgsAssignNodeF)
    //  "getMsgsAssignNodeP" - test(getMsgsAssignNodeP)
    //  "getMsgAssignWorkerQ" - test(getMsgAssignWorkerQ)
    "reassignWorkerQ" - test(reassignWorkerQ)
    "failAndRetryQ" - test(failAndRetryQ)
    //  "archiveMsgQ" - test(archiveMsgQ)

  }
}
