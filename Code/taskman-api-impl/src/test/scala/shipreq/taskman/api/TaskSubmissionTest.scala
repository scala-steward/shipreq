package shipreq.taskman.api

import org.specs2.mutable.Specification
import shipreq.base.test.db.specs2.DatabaseTest

class TaskSubmissionTest extends Specification with DatabaseTest {

  "blah" in {
    val count = sql"select count(1) from task".as[Int].first
    println("Get: " + session)
    println("Count: " + count)
    1 ==== 1
  }

  "blah2" in {
    println("Get: " + session)
    1 ==== 1
  }

}
