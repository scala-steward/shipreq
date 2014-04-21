package shipreq.taskman.server.app

import shipreq.taskman.server.business.MailChimp

object TmpMailchimp extends MainTemplate {

  def main(args: Array[String]): Unit =
    withTaskmanCtx { ctx =>
      ctx.logContent()
      val mi = ctx.mailchimp
      val io1 = mi.run(MailChimp.API.GetListId(ctx.props.mailchimp.masterList))
      val io2 = mi.run(MailChimp.API.GetListId("xx"))

      log info "Ready...."
      log info s"1) ${io1.unsafePerformIO()}"
      log info s"2) ${io2.unsafePerformIO()}"
    }

  object MailChimpTmp {
    object MasterList {

      sealed abstract class Field[V](val tag: String)
      // object EmailAddress extends Field[String]("")
      object Name extends Field[String]("NAME")
      object Newsletter extends Field[BoolAsNum]("NEWSLETTER")
      object AccountStatus extends Field[AccountStatusValue]("ACCT")

      sealed trait BoolAsNum
      object BoolAsNum {
        case object Yes extends BoolAsNum
        case object No extends BoolAsNum
      }

      sealed trait AccountStatusValue
      object AccountStatusValue {
        case object Never extends AccountStatusValue
        case object Active extends AccountStatusValue
      }
    }
  }
}
