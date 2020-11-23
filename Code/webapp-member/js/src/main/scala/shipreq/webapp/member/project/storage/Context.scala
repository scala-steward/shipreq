package shipreq.webapp.member.project.storage

import shipreq.webapp.base.data.{ProjectId, UserId}

final case class Context(userId   : UserId.Public,
                         projectId: ProjectId.Public) {

  val namespace: String =
    s"${userId.value}:${projectId.value}"
}
