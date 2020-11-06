package shipreq.webapp.base.data

final case class IP(value: String)

object IP {
  implicit def univEq: UnivEq[IP] = UnivEq.derive
}
