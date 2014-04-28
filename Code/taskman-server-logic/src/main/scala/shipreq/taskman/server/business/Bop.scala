package shipreq.taskman.server.business

/**
 * Business Operation.
 * An operation in the domain of business logic.
 */
sealed trait Bop[A]

object Bop {

  /** Send an email. */
  case class SendEmail(e: Email.Envelope, c: Email.Content) extends Bop[Unit]

  /** Manage the mailing list. */
  case class MailingListOp[A](op: MailingList.API[A]) extends Bop[A]
}
