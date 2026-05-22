package shipreq.webapp.member.project.data

import shipreq.base.util._
import shipreq.webapp.base.data.{ProjectCreator, ProjectRole, UserId}

final case class ProjectAccess(asMap: Map[UserId, ProjectRole]) {

  def apply(user: UserId): Option[ProjectRole] =
    asMap.get(user)

  def need(user: UserId): ProjectRole =
    apply(user).get

  def adminIterator(): Iterator[UserId] =
    asMap.iterator.filter(_._2 ==* ProjectRole.Admin).map(_._1)

  def hasAdmin: Boolean =
    adminIterator().nonEmpty

  def update(updates: Map[UserId, Option[ProjectRole]]): ProjectAccess = {
    var m = asMap
    updates.foreach {
      case (u, None)    => m -= u
      case (u, Some(p)) => m = m.updated(u, p)
    }
    ProjectAccess(m)
  }

  /** Checks if the given user has the required permission. */
  def require(requiredRole: ProjectRole, user: UserId): Permission =
    requiredRole.isSatisfiedBy(apply(user))

  def requirePC(requiredRole: ProjectRole, user: UserId): PotentialChange[ErrorMsg, Unit] =
    require(requiredRole, user) match {
      case Allow => PotentialChange.unit
      case Deny  => PotentialChange.Failure(requiredRole.errorMsgWhenUnsatisfied)
    }
}

object ProjectAccess {
  implicit def univEq: UnivEq[ProjectAccess] = UnivEq.derive

  def empty: ProjectAccess =
    apply(Map.empty)

  def init(c: ProjectCreator): ProjectAccess =
    apply(Map.empty[UserId, ProjectRole].updated(c.userId, ProjectRole.Admin))
}
