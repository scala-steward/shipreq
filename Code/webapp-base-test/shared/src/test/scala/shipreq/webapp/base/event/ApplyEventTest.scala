package shipreq.webapp.base.event

import scalaz.{-\/, \/-}
import utest._
import shipreq.webapp.base.data._
import shipreq.webapp.base.hash.HashRec
import shipreq.base.test.BaseTestUtil._

object ApplyEventTest extends TestSuite {

  /*
//  case class State(reqCodes: Int, events: Vector[Event])

  case class State(reqCodes: Int)

  object RandomEventStream extends StateGen.Fix[State] {

    val initialState = State(0, Vector.empty)

    // different scopes
    // different hashScheme
    // different logicVer

    def blah(s: State): Gen[State] = {
      val rcn = s.reqCodes + 1
      val rcv = NonEmptyVector one ReqCode.Node(rcn.toString)
      val e = CreateReqCodeGroup(ReqCodeId(rcn), ReqCodeGroupGD.Code(rcv))
      val s2 = s.copy(reqCodes = rcn, events = s.events :+ e)
      Gen pure s2
    }

    //    genS()


  }
*/


  override def tests = TestSuite {
    'applyVerified {

      val p1 = Project.empty
      val e1 = DeleteStaticField(StaticField.StepGraph)

      val p2 = ApplyEvent.untrusted.apply1(e1)(p1) match {
        case \/-(p) => p
        case -\/(x) => fail(s"Init failed: $x")
      }

      val hrs = HashRec.changes(p1, p2)
      assert(hrs.nonEmpty)
      val ve = VerifiedEvent(e1, hrs)

      'pass {
        ApplyEvent.untrusted.applyVerified(List(ve))(p1) match {
          case \/-(p) => assertEq(p, p2)
          case -\/(e) => fail(s"applyVerified failed: $e")
        }
      }

      'fail {
        val vef = ve.copy(hashRecs = ve.hashRecs.map(r => HashRec(r.scope, r.logicVer, r.scheme)(r.hash.map(_ + 1))))
        ApplyEvent.untrusted.applyVerified(List(vef))(p1) match {
          case \/-(p) => fail(s"applyVerified passed when it shouldn't have.")
          case -\/(e) => e
        }
      }
    }
    'stuff {
      val (s,v) = RandomEventStream.withEventStats()(100).samples().next()
      println(s.report)
      ()
    }
  }
}
