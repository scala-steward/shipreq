package shipreq.taskman.server.business

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scalaz.NonEmptyList
import shipreq.taskman.api.Types
import shipreq.taskman.server.MsgDetail
import shipreq.base.util.Error

object Email {

  trait Ctx[EA] {
    def addrParser: AddrParser[EA]

    // Email addresses
    val publicFrom: EA
    val supportEnv: Envelope[EA]

    // Email content tokens
    val shipreq: String
    val loginUrl: String
  }

  type AddrParser[EA] = Types.EmailAddr => EA

  final case class Envelope[EA](from: EA, to: NonEmptyList[EA], cc: List[EA] = Nil, bcc: List[EA] = Nil) {
    override def toString = {
      val sb = new StringBuilder(getClass.getSimpleName)
      def kv(k: String, v: Any, p: String = ", "): Unit = {
        sb append p
        sb append k
        sb append " = "
        sb append v
      }
      def kvo(k: String, v: List[EA]): Unit = if (v.nonEmpty) kv(k, v)
      kv("from", from, "(")
      kv("to", to)
      kvo("cc", cc)
      kvo("bcc", bcc)
      sb append ')'
      sb.toString
    }
  }

  final case class Content(subject: String, body: String)

  val timeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
}

final class Emails[EA](ctx: Email.Ctx[EA]) {
  import Email._
  import ctx._

  type SendOp = Bop.SendEmail[EA]

  def sendToUser(addr: EA, c: Content): SendOp = {
    val e = Email.Envelope(ctx.publicFrom, NonEmptyList(addr))
    Bop.SendEmail(e, c)
  }

  def diagnosticEmail(subject: String, body: String, msg: MsgDetail) =
    Content(s"[DIAG] $subject", s"$body\n\n${"=" * 40}\nMsg header: ${msg.hdr}\nFailure count: ${msg.failureCount}")

  // ===================================================================================================================

  def notifySupportOfWorkerFailure(t: DateTime, m: MsgDetail, e: Error): SendOp =
    Bop.SendEmail(supportEnv, Content(
      s"[TASKMAN] Worker failed on #${m.hdr.id.value}",
      s"TIME: ${t toString timeFormat}\n\nMSG: $m\n\nERROR: ${e.stackTraceStr}"))

  def notifySupportOfTaskmanError(t: DateTime, e: Error, m: Option[MsgDetail]): SendOp =
    Bop.SendEmail(supportEnv, Content(
      s"[TASKMAN] Taskman infrastructure itself failed",
      s"TIME: ${t toString timeFormat}\n\nERROR: ${e.stackTraceStr}\n\nMSG: $m"))

  // ===================================================================================================================

  private val passwordChangeRequestS = s"$shipreq Password Change Request"

  def passwordChangeRequest(url: String) =
    Content(passwordChangeRequestS, s"""
Hi,

Someone recently requested a password change to your $shipreq account.

If this was you, you can set a new password here:
$url

If you didn't request this, please ignore this email - your password will not be changed.

    """.trim)

  // ===================================================================================================================

  private val registrationS = s"Registration at $shipreq"

  def linkToCompleteRegistration(url: String) =
    Content(registrationS, s"""

Your email address has been used to register a $shipreq account.

To continue your registration, simply click on the following link:
$url

If you were not expecting this message, please ignore and delete it.

    """.trim)

  // ===================================================================================================================

  val reRegistrationAttempted =
    Content(registrationS, s"""

Somebody, probably you, has tried to re-register your email address.
As you already have a registered account, no action has been taken.

To login or reset your password, simply click on the following link:
$loginUrl

If you were not expecting this message, please ignore and delete it.

    """.trim)

}