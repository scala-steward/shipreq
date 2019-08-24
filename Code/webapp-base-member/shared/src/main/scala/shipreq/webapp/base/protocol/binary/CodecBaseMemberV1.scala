package shipreq.webapp.base.protocol.binary

import boopickle.DefaultBasic._
import java.time.Instant
import shipreq.webapp.base.data._

object CodecBaseMemberV1 {
  import CodecBaseV1._

  implicit lazy val picklerProjectIdPublic: Pickler[ProjectId.Public] =
    pickleObfuscated

  implicit lazy val picklerProjectMetaData: Pickler[ProjectMetaData] =
    new Pickler[ProjectMetaData] {
      override def pickle(a: ProjectMetaData)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.name)
        state.pickle(a.initEventCount)
        state.pickle(a.totalEventCount)
        state.pickle(a.reqCount)
        state.pickle(a.createdAt)
        state.pickle(a.lastUpdatedAt)
      }
      override def unpickle(implicit state: UnpickleState): ProjectMetaData = {
        val id              = state.unpickle[ProjectId.Public]
        val name            = state.unpickle[Project.Name]
        val initEventCount  = state.unpickle[Int]
        val totalEventCount = state.unpickle[Int]
        val reqCount        = state.unpickle[Int]
        val createdAt       = state.unpickle[Instant]
        val lastUpdatedAt   = state.unpickle[Option[Instant]]
        ProjectMetaData(id, name, initEventCount, totalEventCount, reqCount, createdAt, lastUpdatedAt)
      }
    }
}
