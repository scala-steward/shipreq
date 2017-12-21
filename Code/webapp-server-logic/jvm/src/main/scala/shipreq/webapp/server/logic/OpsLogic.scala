package shipreq.webapp.server.logic

import upickle.Js
import scalaz.Monad
import scalaz.syntax.monad._
import shipreq.taskman.api.{MsgId, TaskmanApi}

trait OpsLogic[F[_]] {
  import OpsLogic._

  def taskmanMsgStatus(id: MsgId): F[Option[MsgStatusResult]]

}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object OpsLogic {

  def apply[F[_]](implicit F: Monad[F],
                  taskman: TaskmanApi[F]): OpsLogic[F] =
    new OpsLogic[F] {

      override def taskmanMsgStatus(id: MsgId) =
        taskman.queryMsgStatus(id).map(_.map(status =>
          MsgStatusResult(id.value, status.toString, status.isArchived)))

    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  private def jsBool(b: Boolean): Js.Value =
    if (b) Js.True else  Js.False

  final case class MsgStatusResult(id: Long, status: String, archived: Boolean) {
    def toJsValue: Js.Value =
      Js.Obj(
        "id" -> Js.Num(id),
        "status" -> Js.Str(status),
        "archived" -> jsBool(archived))
  }
}