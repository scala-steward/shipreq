package shipreq.taskman.server.business

import scalaz.~>
import scalaz.effect.IO
import shipreq.base.util.{ErrorOr, Error}
import shipreq.taskman.api.Msg._
import shipreq.taskman.api.Types.EmailAddr
import shipreq.taskman.server.{Deliberate, Deterministic}
import shipreq.taskman.server.Worker.{MsgProcessor, MsgProcessorIn, MsgProcessorOut}

object BusinessLogic {
  type NoAsync[A] = Nothing
}
import BusinessLogic._

final class BusinessLogic[EA](ctx: Email.Ctx[EA], bopReifier: BopReifier) extends MsgProcessor[NoAsync] {
  type F[A] = NoAsync[A]
  type MI = MsgProcessorIn[F]
  type MO = MsgProcessorOut[F]
  def emailScheduler: IO ~> F = ??? // TODO

  private[this] val emails = new Emails[EA](ctx)
  private[this] implicit def autoParseEa(ea: EmailAddr): EA = ctx.addrParser(ea)
  @inline private[this] def emailUser(to: EA, c: Email.Content)(implicit i: MI): MO = send(emails.sendToUser(to, c))
  @inline private[this] def send(e: Bop.SendEmail[EA])(implicit i: MI): MO = i.syncU(bopReifier(e))

  override def apply(i: MI): MO = {
    @inline def md = i.m
    @inline implicit def _i = i

    md.msg match {

      case RegistrationRequested(addr, url) =>
        emailUser(addr, emails.linkToCompleteRegistration(url))

      case ReRegistrationAttempted(addr) =>
        emailUser(addr, emails.reRegistrationAttempted)

      case PasswordResetRequested(addr, url) =>
        emailUser(addr, emails.passwordChangeRequest(url))

      case SendDiagEmail(addr, subject, body) =>
        emailUser(addr, emails.diagnosticEmail(subject, body, md))

      case DummyMsg(desc, processingTimeMs, retryCount, _, failureMsg) =>
        i.sync {
          if (processingTimeMs > 0)
            Thread.sleep(processingTimeMs)
          ErrorOr.tag[Unit](Deliberate)(
            if (md.failureCount < retryCount)
              Error(s"Failure count (${md.failureCount}) < desired ($retryCount).")
            else failureMsg match {
              case Some(e) => ErrorOr.tag(Deterministic)(Error(e))
              case None    => ErrorOr.unit
            }
          )
      }

    }
  }
}
