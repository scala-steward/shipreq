package shipreq.taskman.server.business

import scalaz.effect.IO
import shipreq.base.util.{ErrorOr, Error}
import shipreq.taskman.api.Msg
import shipreq.taskman.api.Types.EmailAddr
import shipreq.taskman.server.{Deliberate, Deterministic, Worker}
import shipreq.taskman.server.Worker.MsgProcessor

object BusinessLogic {

  def apply[EA](ctx: Email.Ctx[EA], reifier: BopReifier): MsgProcessor = {

    val email = new Emails[EA](ctx)

    implicit def autoReifyBop(bop: Bop[Unit]) = reifier(bop)
    implicit def autoParseEa(ea: EmailAddr): EA = ctx.addrParser(ea)

    val msgProcessor: MsgProcessor =
      md => md.msg match {

        case Msg.RegistrationRequested(addr, url) =>
          email.sendToUser(addr, email.linkToCompleteRegistration(url))

        case Msg.ReRegistrationAttempted(addr) =>
          email.sendToUser(addr, email.reRegistrationAttempted)

        case Msg.PasswordResetRequested(addr, url) =>
          email.sendToUser(addr, email.passwordChangeRequest(url))

        case Msg.SendDiagEmail(addr, subject, body) =>
          email.sendToUser(addr, email.diagnosticEmail(subject, body, md))

        case Msg.DummyMsg(desc, processingTimeMs, retryCount, _, failureMsg) => IO {
          if (processingTimeMs > 0)
            Thread sleep processingTimeMs
          ErrorOr.tag[Unit](Deliberate)(
            if (md.failureCount < retryCount)
              Error(s"Failure count (${md.failureCount}) < desired ($retryCount).")
            else failureMsg match {
              case Some(e) => ErrorOr.tag(Deterministic)(Error(e))
              case None    => Worker.nopResult
            }
          )
        }

      }
    msgProcessor
  }
}
