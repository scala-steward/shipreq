package shipreq.taskman.server.business

import scalaz.effect.IO
import shipreq.base.util.{ErrorOr, Error, Logger}
import shipreq.taskman.api.Msg
import shipreq.taskman.server.Worker
import shipreq.taskman.server.Worker.MsgProcessor

object BusinessLogic extends Logger {

  def apply(ctx: Email.Ctx, reifier: BopReifier): MsgProcessor = {

    val email = new Emails(ctx)

    implicit def autoReifyBop(bop: Bop[Unit]) = reifier(bop)

    val msgProcessor: MsgProcessor =
      md => md.msg match {

        case Msg.RegistrationRequested(addr, url) =>
          email.sendToUser(addr, email.linkToCompleteRegistration(url))

        case Msg.ReRegistrationAttempted(addr) =>
          email.sendToUser(addr, email.reRegistrationAttempted)

        case Msg.PasswordResetRequested(addr, url) =>
          email.sendToUser(addr, email.passwordChangeRequest(url))

        case Msg.DummyMsg(desc, processingTimeMs, retryCount, failureMsg) =>
          IO {
            log.info("Received dummy: {}", desc)
            if (processingTimeMs > 0) Thread sleep processingTimeMs
            val r: ErrorOr[Unit] =
              (md.failureCount < retryCount, failureMsg) match {
                case (true, _)        => Error(s"Retrying: Failure count (${md.failureCount}) < desired ($retryCount).")
                case (false, Some(e)) => Error(e)
                case (false, None)    => Worker.nopResult
              }
            log.info("Fate of dummy: {} => {}", desc, r, null)
            r
          }

      }

    msgProcessor
  }
}
