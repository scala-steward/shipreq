package shipreq.taskman.server.business

import scalaz.NonEmptyList
import shipreq.taskman.api.Types
import shipreq.taskman.server.MsgDetail

object Email {

  trait Ctx[EA] {
    val shipreq: String
    val loginUrl: String
    val defaultFromAddress: EA
    def addrParser: AddrParser[EA]
  }

  type AddrParser[EA] = Types.EmailAddr => EA

  case class Envelope[EA](from: EA
                      , to: NonEmptyList[EA]
                      , cc: List[EA] = Nil
                      , bcc: List[EA] = Nil)

  case class Content(subject: String, body: String)
}

class Emails[EA](ctx: Email.Ctx[EA]) {
  import Email.Content
  import ctx._

  def sendToUser(addr: EA, c: Content): Bop[Unit] = {
    val e = Email.Envelope[EA](ctx.defaultFromAddress, NonEmptyList(addr))
    Bop.SendEmail(e, c)
  }

  def diagnosticEmail(subject: String, body: String, msg: MsgDetail) =
    Content(s"[DIAG] $subject", s"$body\n\n${"=" * 40}\nMsg header: ${msg.hdr}\nFailure count: ${msg.failureCount}")

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