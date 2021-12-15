package shipreq.webapp.member.project.protocol.binary.v1

import boopickle.DefaultBasic._
import java.time.Instant
import shipreq.webapp.base.data.{ProjectId, ProjectPerm}
import shipreq.webapp.member.project.data._

/** This is extra stuff added in Phase 3.
  */
object BaseMemberData3 {
  import shipreq.webapp.base.protocol.binary.v1.BaseData._

  implicit lazy val picklerProjectPerm: Pickler[ProjectPerm] =
    new Pickler[ProjectPerm] {
      private[this] final val KeyAdmin        = 0
      private[this] final val KeyCollaborator = 1
      override def pickle(a: ProjectPerm)(implicit state: PickleState): Unit =
        a match {
          case ProjectPerm.Admin        => state.enc.writeByte(KeyAdmin       )
          case ProjectPerm.Collaborator => state.enc.writeByte(KeyCollaborator)
        }
      override def unpickle(implicit state: UnpickleState): ProjectPerm =
        state.dec.readByte match {
          case KeyAdmin        => ProjectPerm.Admin
          case KeyCollaborator => ProjectPerm.Collaborator
        }
    }


  implicit lazy val picklerProjectMetaData: Pickler[ProjectMetaData] =
    new Pickler[ProjectMetaData] {
      override def pickle(a: ProjectMetaData)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.perm)
        state.pickle(a.name)
        state.pickle(a.eventsInit)
        state.pickle(a.eventsTotal)
        state.pickle(a.reqsLive)
        state.pickle(a.reqsTotal)
        state.pickle(a.createdAt)
        state.pickle(a.accessedAt)
        state.pickle(a.lastUpdatedAt)
      }
      override def unpickle(implicit state: UnpickleState): ProjectMetaData = {
        val id            = state.unpickle[ProjectId.Public]
        val perm          = state.unpickle[ProjectPerm]
        val name          = state.unpickle[Project.Name]
        val eventsInit    = state.unpickle[Int]
        val eventsTotal   = state.unpickle[Int]
        val reqsLive      = state.unpickle[Int]
        val reqsTotal     = state.unpickle[Int]
        val createdAt     = state.unpickle[Instant]
        val accessedAt    = state.unpickle[Instant]
        val lastUpdatedAt = state.unpickle[Option[Instant]]
        ProjectMetaData(
          id            = id           ,
          perm          = perm         ,
          name          = name         ,
          eventsInit    = eventsInit   ,
          eventsTotal   = eventsTotal  ,
          reqsLive      = reqsLive     ,
          reqsTotal     = reqsTotal    ,
          createdAt     = createdAt    ,
          accessedAt    = accessedAt   ,
          lastUpdatedAt = lastUpdatedAt)
      }
    }

}
