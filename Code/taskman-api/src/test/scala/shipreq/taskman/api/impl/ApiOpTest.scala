package shipreq.taskman.api.impl

import doobie.imports._
import utest._
import shipreq.base.test.db.TestDb
import shipreq.base.util.FxModule._
import shipreq.taskman.api.{EmailAddr, Msg, MsgId, MsgStatus}

object ApiOpTest extends TestSuite with ApiImplTestHelpers {

  override def tests = Tests {

    "Task submission" - {
      "Submits a task" - {
        val r: Int = TestDb() { xa =>
          for {
            _ <- taskmanApi(xa).submitMsg(Msg.RegistrationRequested(EmailAddr("a@b.com"), "http://x"))
            c <- Query0[Int]("select count(1) from msgq").unique.transact(xa)
          } yield c
        }.unsafeRun()
        assert(r == 1)
      }
    }

    "Query msg status" - {

      "When msg doesn't exist" - {
        val r = run(_.queryMsgStatus(MsgId(123456)))
        assert(r == None)
      }

      "On new msg" - {
        val r = run(api =>
          for {
            id <- api.submitMsg(Msg.RegistrationRequested(EmailAddr("a@b.com"), "http://x"))
            s <- api.queryMsgStatus(id)
          } yield s
        )
        assert(r == Some(MsgStatus.Unassigned))
      }
    }

  }
}
