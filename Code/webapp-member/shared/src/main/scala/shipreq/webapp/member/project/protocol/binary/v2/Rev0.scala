package shipreq.webapp.member.project.protocol.binary.v2

import java.time.Instant
import shipreq.webapp.base.data._
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.event._

/** v2.0: For ShipReq Phase 3. */
object Rev0 {
  import boopickle.DefaultBasic._
  import shipreq.webapp.base.protocol.binary.v1.BaseData._

  implicit lazy val picklerProjectCreator: Pickler[ProjectCreator] =
    implicitly[Pickler[UserId]].xmap(ProjectCreator.apply)(_.userId)

  implicit lazy val picklerProjectRole: Pickler[ProjectRole] =
    new Pickler[ProjectRole] {
      // Note: 0 is reserved for Option[ProjectRole]
      private[this] final val KeyAdmin        = 1
      private[this] final val KeyCollaborator = 2
      private[this] final val KeyViewer       = 3
      override def pickle(a: ProjectRole)(implicit state: PickleState): Unit =
        a match {
          case ProjectRole.Admin        => state.enc.writeByte(KeyAdmin       )
          case ProjectRole.Collaborator => state.enc.writeByte(KeyCollaborator)
          case ProjectRole.Viewer       => state.enc.writeByte(KeyViewer      )
        }
      override def unpickle(implicit state: UnpickleState): ProjectRole =
        state.dec.readByte match {
          case KeyAdmin        => ProjectRole.Admin
          case KeyCollaborator => ProjectRole.Collaborator
          case KeyViewer       => ProjectRole.Viewer
        }
    }

  implicit lazy val picklerOptionProjectRole: Pickler[Option[ProjectRole]] =
    new Pickler[Option[ProjectRole]] {
      private[this] final val KeyNone = 0
      override def pickle(a: Option[ProjectRole])(implicit state: PickleState): Unit =
        a match {
          case Some(p) => picklerProjectRole.pickle(p)
          case None    => state.enc.writeByte(KeyNone)
        }
      override def unpickle(implicit state: UnpickleState): Option[ProjectRole] =
        if (state.dec.peek(_.readByte) == KeyNone) {
          state.dec.readByte
          None
        } else
          Some(picklerProjectRole.unpickle)
    }

  private[binary] implicit lazy val picklerEventAccessUpdate: Pickler[Event.AccessUpdate] =
    new Pickler[Event.AccessUpdate] {
      override def pickle(a: Event.AccessUpdate)(implicit state: PickleState): Unit = {
        state.pickle(a.userId)
        state.pickle(a.newRole)
      }
      override def unpickle(implicit state: UnpickleState): Event.AccessUpdate = {
        val userId  = state.unpickle[UserId]
        val newRole = state.unpickle[Option[ProjectRole]]
        Event.AccessUpdate(userId, newRole)
      }
    }

  implicit lazy val picklerClientSideProjectEncryptionKey: Pickler[ClientSideProjectEncryptionKey] =
    transformPickler(ClientSideProjectEncryptionKey.apply)(_.value)

  implicit lazy val picklerProjectAccess: Pickler[ProjectAccess] =
    pickleMap[UserId, ProjectRole].xmap(ProjectAccess.apply)(_.asMap)

  implicit lazy val picklerProjectMetaData: Pickler[ProjectMetaData] =
    new Pickler[ProjectMetaData] {
      override def pickle(a: ProjectMetaData)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.role)
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
        val role          = state.unpickle[Option[ProjectRole]]
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
          role          = role         ,
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

  implicit lazy val picklerRolodex: Pickler[Rolodex] =
    pickleMap[UserId, Username].xmap(Rolodex.apply)(_.asMap)

}
