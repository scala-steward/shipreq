package shipreq.webapp.base.data

import scalaz.Equal
import scalaz.Isomorphism.<=>
import shipreq.base.util.TaggedTypes._


final case class Rev(value: Long) extends TaggedLong {
  @inline def succ      = Rev(value + 1L)
  @inline def +(r: Rev) = Rev(value + r.value)
}


sealed trait Alive
case object Alive extends Alive with (Boolean <=> Alive) {
  implicit val equality = Equal.equalA[Alive]
  override val from     = equality.equal(Alive, _: Alive)
  override val to       = if (_: Boolean) Alive else Dead
}
case object Dead extends Alive


sealed trait ImplicationRequired
case object ImplicationRequired extends ImplicationRequired with (Boolean <=> ImplicationRequired) {
  implicit val equality = Equal.equalA[ImplicationRequired]
  override val from     = equality.equal(ImplicationRequired, _: ImplicationRequired)
  override val to       = if (_: Boolean) ImplicationRequired else Not
  case object Not extends ImplicationRequired
}


/**
 * A key by which users can insert references to corresponding data.
 *
 * Examples:
 * #TBD refers to a custom issue type.
 * #pri=high refers to a grouping.
 */
final case class RefKey(value: String) extends TaggedString
