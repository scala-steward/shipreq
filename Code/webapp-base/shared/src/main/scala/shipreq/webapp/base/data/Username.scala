package shipreq.webapp.base.data

final case class Username(value: String) extends AnyVal {
  def with_@ : String =
    "@" + value
}
