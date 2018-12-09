package shipreq.taskman.server.logic

import java.time.Duration
import java.time.temporal.ChronoUnit
import shipreq.base.util.FxModule._
import shipreq.base.test.MockOpTransformer1
import utest._
import TestHelpers._
import ServerOp._

object SourceTest extends TestSuite {

  implicit val node = NodeId(8)
  val mockMsgs = List(mh_1, mh_2)
  implicit val mockSop = MockOpTransformer1[ServerOp, Fx, GetMsgsAssignNode, List[MsgHeader]](SopTypeTags, mockMsgs)
  implicit val clock = Fx(timeNow)
  implicit val tp = AssignmentTrustPeriod(Duration ofDays 3)
  val source = new Source(Duration ofSeconds 1, 20)

  override def tests = Tests {

  "poll when allowed" - {
    val (s, ms) = source.poll(None).run(timeNow.minus(1, ChronoUnit.DAYS)).unsafeRun()
    "Time should be updated"    - { s ==> timeNow }
    "Retrieves message headers" - { ms ==> mockMsgs }
  }

  "poll before pollGap expires" - {
    val lastPolled = timeNow minusNanos 1
    val (s, ms) = source.poll(None).run(lastPolled).unsafeRun()
    "Time should not be updated"       - { s ==> lastPolled }
    "Doesn't retrieve message headers" - { ms ==> Nil }
  }
  }

}
