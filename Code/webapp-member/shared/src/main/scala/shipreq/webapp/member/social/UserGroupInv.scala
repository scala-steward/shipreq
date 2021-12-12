package shipreq.webapp.member.social

import java.time.Instant
import shipreq.webapp.base.util.Obfuscated

final case class UserGroupInv[+Id, +U, +UG](id        : Id,
                                            inviter   : U,
                                            invitee   : U,
                                            userGroup : UG,
                                            perm      : UserGroup.Perm,
                                            createdAt : Instant,
                                            conclusion: Option[UserGroupInv.Conclusion])

object UserGroupInv {

  final case class Id(value: Long)

  object Id {
    /** The real UserGroupInv.Id is never directly exposed to users. Publicly it has a different ID. */
    type Public = Obfuscated[Id]
  }

  final case class Conclusion(how: ConclusionType, when: Instant)

  sealed trait ConclusionType
  object ConclusionType {
    case object Accepted extends ConclusionType
    case object Rejected extends ConclusionType
    case object Revoked  extends ConclusionType
  }

  final case class Target[+U](invitee: U, perm: UserGroup.Perm) {
    def map[A](f: U => A): Target[A] =
      copy(invitee = f(invitee))
  }

  @inline implicit def univEq  [A: UnivEq, B: UnivEq, C: UnivEq]: UnivEq[UserGroupInv[A, B, C]] = UnivEq.derive
  @inline implicit def univEqC                                  : UnivEq[Conclusion           ] = UnivEq.derive
  @inline implicit def univEqCT                                 : UnivEq[ConclusionType       ] = UnivEq.derive
  @inline implicit def univEqId                                 : UnivEq[Id                   ] = UnivEq.derive
  @inline implicit def univEqT [A: UnivEq]                      : UnivEq[Target[A]            ] = UnivEq.derive
}
