package shipreq.webapp.base.data

final case class Username(value: String) {
  def with_@ : String =
    "@" + value
}

object Username {
  implicit def univEq: UnivEq[Username] = UnivEq.derive

  def orEmail(usernameOrEmail: String): Username \/ EmailAddr =
    if (EmailAddr.isEmailAddr(usernameOrEmail))
      \/-(EmailAddr(usernameOrEmail))
    else
      -\/(Username(usernameOrEmail))
}
