package shipreq.taskman.server.app

import shipreq.taskman.server.business.MailChimp._
import shipreq.taskman.server.business.MailChimp.API._
import shipreq.taskman.api.Types._
import scalaz.NonEmptyList
import shipreq.base.util.effect.IOE
import scalaz.effect.IO
import shipreq.base.util.ErrorOr.Implicits._

object TmpMailchimp extends MainTemplate {

  def main(args: Array[String]): Unit =
    withTaskmanCtx { ctx =>
      ctx.logContent()
      val mi = ctx.mailchimp
      log info "Ready...."

      val io1 = mi.run(GetListId(ctx.props.mailchimp.masterList))
      val io2 = (o: Option[ListId]) => IOE(o.get)
      val io3 = (id: ListId) => IO(log info s"ID: $id")
      val io4 = (id: ListId) => {
          val s = Subscription("tmp-mailchimp-app@shipreq.com".tag, "Tmp MailChimp App", true, AccountStatus.Never)
          mi.run(BatchSubscribe(id, NonEmptyList(s)))
        }
      val io = io1 >==> io2 <<| io3 >==> io4
      io.unsafePerformIO()
    }
}
