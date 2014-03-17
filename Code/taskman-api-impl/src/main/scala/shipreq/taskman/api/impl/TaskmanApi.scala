package shipreq.taskman.api.impl

import scala.slick.session.Session
import scalaz.~>
import shipreq.taskman.api.ApiOp
import ApiOp._
import ApiOp.Effect._

object TaskmanApiImpl {

  class GlobalContext(schema: Option[String]) {
    private[impl] val sql = new ApiSql(schema.map(_ + ".") getOrElse "")
  }

  def reify(ctx: GlobalContext, s: Session): (ApiOp ~> IOM) =
    new (ApiOp ~> IOM) {
      private[this] def newDao = new ApiDao(ctx, s)
      def apply[A](c: ApiOp[A]): IOM[A] = c match {

        case SubmitMsg(t) => io {
          newDao.createMsg(t)
        }

        case SubmitMsgs(ts) => io {
          val dao = newDao
          ts.foreach(t => dao.createMsg(t))
        }

      }
    }

}
