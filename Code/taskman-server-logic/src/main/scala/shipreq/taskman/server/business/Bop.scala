package shipreq.taskman.server.business

/**
 * Business Operation.
 * An operation in the domain of business logic.
 */
sealed trait Bop[A]

object Bop {

  case class SendEmail[EA](e: Email.Envelope[EA], c: Email.Content) extends Bop[Unit]
}
