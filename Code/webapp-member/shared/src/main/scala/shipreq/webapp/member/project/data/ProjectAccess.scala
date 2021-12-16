package shipreq.webapp.member.project.data

import shipreq.webapp.base.data.{ProjectPerm, UserId}

final case class ProjectAccess(value: Map[UserId.Public, ProjectPerm]) {

  def update(updates: Map[UserId.Public, Option[ProjectPerm]]): ProjectAccess = {
    var m = value
    updates.foreach {
      case (u, None)    => m -= u
      case (u, Some(p)) => m = m.updated(u, p)
    }
    ProjectAccess(m)
  }
}

object ProjectAccess {
  implicit def univEq: UnivEq[ProjectAccess] = UnivEq.derive

  val empty = apply(Map.empty)
}
