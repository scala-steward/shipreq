package shipreq.webapp.base.validation

import scalaz.Endo
import shipreq.webapp.base.TextMod._
import Constraints._

trait ConstraintPlus[A] {
  def constraint: Constraint[A]
  def live: Endo[A]
}
object ConstraintPlus {
  implicit def autoC[A](c: ConstraintPlus[A]) = c.constraint
  implicit def autoE[A](c: ConstraintPlus[A]) = c.live
}

final case class WhitelistCharsR(charRange: String, errMsg: String) extends ConstraintPlus[String] {
  override def constraint = whitelistCharsR(charRange)(errMsg)
  override def live = regexReplace(s"[^$charRange]".r, "")
}

final case class LengthInRange(range: Range.Inclusive) extends ConstraintPlus[String] {
  override def constraint = lengthInRange(range)
  override def live = truncateToLength(range)
}
