package shipreq.webapp.base.data

import shipreq.webapp.base.util.Obfuscated

final case class UserId(value: Long)

object UserId {
  implicit def univEq: UnivEq[UserId] = UnivEq.derive

  /** The real UserId is never directly exposed to users. Publicly it has a different ID. */
  type Public = Obfuscated[UserId]
}
