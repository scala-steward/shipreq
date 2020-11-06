package shipreq.webapp.base.data

final case class User(id      : UserId,
                      username: Username)

object User {
  implicit def univEq: UnivEq[User] = UnivEq.derive
}
