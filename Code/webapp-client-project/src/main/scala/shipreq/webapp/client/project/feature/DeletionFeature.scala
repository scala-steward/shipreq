package shipreq.webapp.client.project.feature

import japgolly.microlibs.nonempty.NonEmptySet
import shipreq.webapp.base.data.{Project, ReqId}

object DeletionFeature {

  // type DeleteOrRestore = deletion.DeleteOrRestore
  // val  DeleteOrRestore = deletion.DeleteOrRestore
  // val  Delete          = deletion.Delete
  // val  Restore         = deletion.Restore
  import deletion.{Delete, Restore}

  type Data = deletion.DeletionRestorationLogic.Data
  val  Data = deletion.DeletionRestorationLogic.Data

  def deletionData(project: Project, directlySelected: NonEmptySet[ReqId]): Data =
    deletion.DeletionRestorationLogic.forReqs(Delete, project, directlySelected)

  def restorationData(project: Project, directlySelected: NonEmptySet[ReqId]): Data =
    deletion.DeletionRestorationLogic.forReqs(Restore, project, directlySelected)

  val DeletionFormProps = deletion.DeletionForm.Props

}
