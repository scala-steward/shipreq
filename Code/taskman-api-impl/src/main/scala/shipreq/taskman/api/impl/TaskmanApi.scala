package shipreq.taskman.api.impl

import scala.slick.session.Session
import scalaz.~>
import shipreq.taskman.FreeEffect._
import shipreq.taskman.api.ApiOp
import ApiOp._

object TaskmanApiImpl {

  class GlobalContext(schema: Option[String]) {
    private[impl] val sql = new ApiSql(schema.map(_ + ".") getOrElse "")
  }

  def reify(ctx: GlobalContext, s: Session): (ApiOp ~> IOM) =
    new (ApiOp ~> IOM) {
      private[this] def dao = new ApiDao(ctx, s)
      def apply[A](c: ApiOp[A]): IOM[A] = c match {

        case SubmitMsg(t) => iom {
          dao.createMsg(t)
        }

        case SubmitMsgs(ts) => iom {
          val _dao = dao
          ts.foreach(t => _dao.createMsg(t))
        }

        case CfgPut(k, v) => iom {
          dao.cfgPut(k, v)
        }

      }
    }

}
