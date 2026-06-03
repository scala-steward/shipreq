package shipreq.webapp.member.project.protocol.binary.v2

import shipreq.webapp.base.protocol.Version
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.event._
import shipreq.webapp.member.project.text.Text.DeletionReason

object Rev1 {
  import boopickle.DefaultBasic._
  import shipreq.webapp.base.protocol.binary.v1.BaseData._
  import shipreq.webapp.member.project.protocol.binary.v1.BaseMemberData2._
  import shipreq.webapp.member.project.protocol.binary.v1.Rev6._
  import shipreq.webapp.member.project.protocol.binary.v1.Rev7.SavedViewPicklers._
  import Rev0._
  import AtomPicklers.instances._

  val latestMinorVersion = Version.Minor(1)

  def picklerProject(v: Version.Minor): Pickler[Project] =
    new Pickler[Project] {
      override def pickle(p: Project)(implicit state: PickleState): Unit = {
        assert(v ==* latestMinorVersion)
        state.pickle(p.name)
        state.pickle(p.config)
        state.pickle(p.content)
        state.pickle(p.manualIssues)
        state.pickle(p.savedViews)
        state.pickle(p.access)
        state.pickle(p.deletionReason)
        state.pickle(p.history)
        state.pickle(p.idCeilings)
      }
      override def unpickle(implicit state: UnpickleState): Project = {
        val name           = state.unpickle[Project.Name]
        val config         = state.unpickle[ProjectConfig]
        val content        = state.unpickle[ProjectContent]
        val manualIssues   = state.unpickle[ManualIssues]
        val savedViews     = state.unpickle[savedview.SavedViews.Optional]
        val access         = state.unpickle[ProjectAccess]
        val deletionReason = if (v.value >= 1) state.unpickle[Option[DeletionReason.OptionalText]] else None
        val history        = state.unpickle[ProjectEvents]
        val idCeilings     = state.unpickle[IdCeilings]
        Project(name, config, content, manualIssues, savedViews, access, deletionReason, history, idCeilings)
      }
    }

  def picklerProjectOrEvents(projectVer: Version.Minor): Pickler[Project \/ VerifiedEvent.Seq] = {
    implicit val p = picklerProject(projectVer)
    pickleDisj
  }

}
