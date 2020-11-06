package shipreq.webapp.base.data

final case class EmailAddr(value: String) {
  def mailto: String =
    "mailto:" + value
}

object EmailAddr {
  implicit def univEq: UnivEq[EmailAddr] = UnivEq.derive

  def isEmailAddr(s: String): Boolean =
  // >0 instead of !=-1 because @golly will be interpreted as a username and email addresses can't start with @
    s.indexOf('@') > 0
}
