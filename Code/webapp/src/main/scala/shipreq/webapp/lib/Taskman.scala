package shipreq.webapp.lib

import scala.slick.session.Session
import shipreq.webapp.app.AppConfig
import shipreq.taskman.api.impl.TaskmanApiImpl._
import shipreq.taskman.api.{Msg, ApiOp}
import ApiOp._
import Effect._

object TaskmanImpl extends TaskmanInterface {

  val ctx = new GlobalContext(Some(AppConfig.TaskmanSchema))

  @inline private def run[A](msg: ApiOp[A], s: Session): A =
    compile(msg, reify(ctx, s)).unsafePerformIO()

  override def submitMsg(m: Msg, s: Session) = run(SubmitMsg(m), s)
  override def submitMsgs(ms: Seq[Msg], s: Session) = run(SubmitMsgs(ms), s)
}

trait TaskmanInterface {
  def submitMsg(m: Msg, s: Session): Unit
  def submitMsgs(ms: Seq[Msg], s: Session): Unit
}
