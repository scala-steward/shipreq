package shipreq.webapp.test

import shipreq.webapp.lib.{TaskmanImpl, TaskmanInterface}
import scala.slick.session.Session
import shipreq.taskman.api._
import shipreq.taskman.api.impl._
import TaskmanApi._
import shipreq.webapp.app.DI

object TestTaskman {
  def install[R](f: => R): (R, TestTaskman) = {
    val tt = new TestTaskman
    val r = DI.Taskman.doWith(tt)(f)
    (r, tt)
  }
}

class TestTaskman extends TaskmanInterface {

  def ctx = TaskmanImpl.ctx

  @inline private def run[A](cmd: Cmd[A]): Unit =
    synchronized {
      ran ::= cmd
      cmd match {
        case SubmitTask(t) => tasksSubmitted ::= t
        case SubmitTasks(ts) => tasksSubmitted :::= ts.toList
      }
    }

  override def submitTask(msg: Msg, s: Session) = run(SubmitTask(msg))
  override def submitTasks(msgs: Seq[Msg], s: Session) = run(SubmitTasks(msgs))

  @volatile var ran: List[Cmd[_]] = List.empty
  @volatile var tasksSubmitted: List[Msg] = List.empty
}
