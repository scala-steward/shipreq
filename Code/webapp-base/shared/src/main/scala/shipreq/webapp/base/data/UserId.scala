package shipreq.webapp.base.data

final case class UserId(value: Long) {
  @inline def valueAsStr: String =
    value.toString
}

object UserId {
  implicit def univEq: UnivEq[UserId] = UnivEq.derive
}
