package shipreq.webapp.lib

import shipreq.webapp.app.AppConfig
import scala.slick.session.Session
import shipreq.taskman.api._
import shipreq.taskman.api.impl._
import TaskmanApiImpl._
import TaskmanApi._
import Effect._

object TaskmanImpl extends TaskmanInterface {

  val ctx = new GlobalContext(Some(AppConfig.TaskmanSchema))

  @inline private def run[A](msg: Cmd[A], s: Session): A =
    compile(msg, reify(ctx, s)).unsafePerformIO()

  override def submitTask(msg: Msg, s: Session) = run(SubmitTask(msg), s)
  override def submitTasks(msgs: Seq[Msg], s: Session) = run(SubmitTasks(msgs), s)
}

trait TaskmanInterface {
  def submitTask(msg: Msg, s: Session): Unit
  def submitTasks(msgs: Seq[Msg], s: Session): Unit
}
