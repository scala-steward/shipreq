package shipreq.webapp.base.data

import java.time.Instant
import shipreq.base.util.univeq._

final case class ProjectMetaData(id           : Project.XId,
                                 name         : Project.Name,
                                 eventCount   : Int,
                                 reqCount     : Int,
                                 createdAt    : Instant,
                                 lastUpdatedAt: Option[Instant]) {

  def lastUpdatedOrCreatedAt: Instant =
    lastUpdatedAt.getOrElse(createdAt)
}

object ProjectMetaData {
  implicit def equality: UnivEq[ProjectMetaData] = UnivEq.derive
}