package shipreq.webapp.test

import scala.slick.session.Session
import scalaz.~>
import shipreq.webapp.app.DI
import shipreq.webapp.lib.{TaskmanImpl, TaskmanInterface}
import shipreq.taskman.api.{MsgId, Msg, ApiOp}
import ApiOp._
import scalaz.effect.IO

object TestTaskman {
  def install[R](f: => R): (R, TestTaskman) = {
    val tt = new TestTaskman
    val r = DI.Taskman.doWith(tt)(f)
    (r, tt)
  }
}

class TestTaskman extends TaskmanInterface {

  def ctx = TaskmanImpl.ctx

  val reify: (ApiOp ~> IO) = new (ApiOp ~> IO) {
    def apply[A](c: ApiOp[A]): IO[A] = synchronized {
      ran ::= c
      c match {
        case SubmitMsg(t)       => IO{ msgsSubmitted ::= t; null.asInstanceOf[MsgId] }
        case SubmitMsgs(ts)     => IO{ msgsSubmitted :::= ts.toList }
        case CfgPut(k, v)       => IO()
        case QueryMsgStatus(id) => IO(None)
      }
    }
  }

  override def run[A](op: ApiOp[A])(s: Session): A =
    synchronized(reify(op).unsafePerformIO())

  @volatile var ran: List[ApiOp[_]] = List.empty
  @volatile var msgsSubmitted: List[Msg] = List.empty
}
