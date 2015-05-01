package shipreq.webapp.client.util

import scalaz.\/
import shipreq.base.util.UnivEq

/** Safer replacement for a boolean */
sealed trait IsOK

case object IsOK extends IsOK {
  def apply      (b: Boolean): IsOK = if (b) IsOK else NotOK
  def apply[L, R](e: L \/ R) : IsOK = IsOK(e.isRight)

  implicit def equality: UnivEq[IsOK] = UnivEq.force
}

case object NotOK extends IsOK
