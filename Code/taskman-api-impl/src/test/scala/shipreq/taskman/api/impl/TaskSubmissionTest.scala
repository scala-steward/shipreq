package shipreq.taskman.api.impl

import org.specs2.mutable.Specification
import scalaz.Free.FreeC
import shipreq.base.test.db.specs2.DatabaseTest
import shipreq.taskman.api.Types._
import shipreq.taskman.api.{Msg, ApiOp}
import shipreq.taskman.FreeEffect._
import ApiOp.SubmitMsg
import TaskmanApiImpl._

class TaskSubmissionTest extends Specification with DatabaseTest {

  def run[A](cmds: FreeC[ApiOp, A]): A =
    compile(cmds, reify(new GlobalContext(None), session)).unsafePerformIO()

  "Submits task" in {
    run(SubmitMsg(Msg.RegistrationRequested("a@b.com".tag, "http://x")))
    sql"select count(1) from msgq".as[Int].first ==== 1
  }

}