package shipreq.taskman.server.business

import shipreq.taskman.api.Msg
import shipreq.taskman.server.Worker.MsgProcessor
import BusinessLogic._

object BusinessLogic {

  implicit def autoReifyBop(bop: Bop[Unit])(implicit reifier: BopReifier) = reifier(bop)
}

class BusinessLogic(implicit ctx: Email.Ctx, reifier: BopReifier) {

  val email = new Emails(ctx)

  val msgProcessor: MsgProcessor = {

    case Msg.RegistrationRequested(addr, url) =>
      email.sendToUser(addr, email.linkToCompleteRegistration(url))

    case Msg.ReRegistrationAttempted(addr) =>
      email.sendToUser(addr, email.reRegistrationAttempted)

    case Msg.PasswordResetRequested(addr, url) =>
      email.sendToUser(addr, email.passwordChangeRequest(url))
  }
}
