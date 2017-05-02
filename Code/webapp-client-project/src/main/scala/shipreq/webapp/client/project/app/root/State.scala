package shipreq.webapp.client.project.app.root

import japgolly.scalajs.react.extra._
import monocle.macros._
import shipreq.base.util._
import shipreq.base.util.univeq._
import shipreq.webapp.base.data.{FilterDead, HideDead}
import shipreq.webapp.client.base.feature._
import shipreq.webapp.client.base.ui.ProjectItem
import shipreq.webapp.client.project.app.state.ClientData
import shipreq.webapp.client.project.app.{reqdetail, reqtable}
import shipreq.webapp.client.project.feature._
import shipreq.webapp.client.project.lib.DataReusability._
import reqdetail.ReqDetail
import reqtable.ReqTable

sealed trait PreviewId
object PreviewId {

  case class Editor(id: EditorFeature.PreviewId) extends PreviewId
  case class ReqTableCI(value: reqtable.PreviewId.InCI) extends PreviewId

  implicit def equality: UnivEq[PreviewId] = UnivEq.derive
  implicit val reusability: Reusability[PreviewId] = Reusability.byUnivEq

  val ToReqTable = Intersection[PreviewId, reqtable.PreviewId] {
    case Editor(e)     => Some(reqtable.PreviewId.InEditor(e))
    case ReqTableCI(a) => Some(a)
  } {
    case reqtable.PreviewId.InEditor(e) => Some(Editor(e))
    case a: reqtable.PreviewId.InCI     => Some(ReqTableCI(a))
  }

  val ToEditor = Intersection[PreviewId, EditorFeature.PreviewId] {
    case Editor(e)     => Some(e)
    case ReqTableCI(_) => None
  }(e => Some(Editor(e)))
}

// █████████████████████████████████████████████████████████████████████████████████████████████████████████████████████

sealed abstract class AsyncKey
object AsyncKey {
  import reqdetail.Row.UseCaseSteps
  import shipreq.webapp.base.data.UseCaseStepId

  /** The req itself. Eg. if a req is being deleted then the entire req should be locked */
  case object WholeReq                             extends AsyncKey
  case class Editor(field: EditorFeature.FieldKey) extends AsyncKey
  case class UseCaseStepCtrls  (id: UseCaseStepId) extends AsyncKey
  case class AddUseCaseStep    (id: UseCaseStepId) extends AsyncKey
  case class AddUseCaseTailStep(s: UseCaseSteps)   extends AsyncKey

  @inline implicit def equality: UnivEq[AsyncKey] =
    UnivEq.derive

  implicit val reusability: Reusability[AsyncKey] =
    Reusability.byUnivEq

  val ToEditor = Intersection[AsyncKey, EditorFeature.FieldKey] {
    case Editor(key)           => Some(key)
    case WholeReq
       | UseCaseStepCtrls  (_)
       | AddUseCaseStep    (_)
       | AddUseCaseTailStep(_) => None
  }(e => Some(Editor(e)))

  val ToReqTable = Intersection[AsyncKey, reqtable.Column] {
    case Editor(key)           => reqtable.Column.editorFieldIntersection.reverse.getOption(key)
    case WholeReq
       | UseCaseStepCtrls  (_)
       | AddUseCaseStep    (_)
       | AddUseCaseTailStep(_) => None
  }(reqtable.Column.editorFieldIntersection.getOption(_).map(Editor))

  val ToReqTable2 = Intersection[AsyncKey, Option[reqtable.Column]] {
    case WholeReq => Some(None)
    case x        => ToReqTable.getOption(x).map(Some(_))
  } {
    case None    => Some(WholeReq)
    case Some(x) => ToReqTable.reverse.getOption(x)
  }

  val ToReqDetail = Intersection[AsyncKey, reqdetail.Cell] {
    case Editor(e) => e match {
      case EditorFeature.FieldKey.ReqType                => Some(reqdetail.Cell.ReqType               )
      case EditorFeature.FieldKey.Code                   => Some(reqdetail.Cell.Code                  )
      case EditorFeature.FieldKey.Title                  => Some(reqdetail.Cell.Title                 )
      case EditorFeature.FieldKey.CustomTextField(field) => Some(reqdetail.Cell.CustomTextField(field))
      case EditorFeature.FieldKey.Tags           (field) => Some(reqdetail.Cell.Tags           (field))
      case EditorFeature.FieldKey.Implications   (scope) => Some(reqdetail.Cell.Implications   (scope))
      case EditorFeature.FieldKey.UseCaseStep    (id)    => Some(reqdetail.Cell.UseCaseStep    (id)   )
    }
    case UseCaseStepCtrls  (id) => Some(reqdetail.Cell.UseCaseStepCtrls  (id))
    case AddUseCaseStep    (id) => Some(reqdetail.Cell.AddUseCaseStep    (id))
    case AddUseCaseTailStep(s)  => Some(reqdetail.Cell.AddUseCaseTailStep(s) )
    case WholeReq               => None // TODO ReqDetail doesn't lock the whole requirement when deleting
  } {
    case reqdetail.Cell.ReqType                => Some(Editor(EditorFeature.FieldKey.ReqType               ))
    case reqdetail.Cell.Code                   => Some(Editor(EditorFeature.FieldKey.Code                  ))
    case reqdetail.Cell.Title                  => Some(Editor(EditorFeature.FieldKey.Title                 ))
    case reqdetail.Cell.CustomTextField(field) => Some(Editor(EditorFeature.FieldKey.CustomTextField(field)))
    case reqdetail.Cell.Tags           (field) => Some(Editor(EditorFeature.FieldKey.Tags           (field)))
    case reqdetail.Cell.Implications   (scope) => Some(Editor(EditorFeature.FieldKey.Implications   (scope)))
    case reqdetail.Cell.UseCaseStep    (id)    => Some(Editor(EditorFeature.FieldKey.UseCaseStep    (id)   ))
    case reqdetail.Cell.UseCaseStepCtrls  (id) => Some(UseCaseStepCtrls  (id))
    case reqdetail.Cell.AddUseCaseStep    (id) => Some(AddUseCaseStep    (id))
    case reqdetail.Cell.AddUseCaseTailStep(s)  => Some(AddUseCaseTailStep(s) )
  }
}

// █████████████████████████████████████████████████████████████████████████████████████████████████████████████████████

@Lenses
case class State(projectName : ProjectItem.WithEditableName.State,
                 reqLookup   : String,
                 editors     : EditorFeature.State.ForProject,
                 async       : AsyncFeature.State.D2[EditorFeature.RowKey, AsyncKey, EditorFeature.AsyncError],
                 preview     : PreviewFeature.State[PreviewId],
                 filterDead  : FilterDead,
                 reqTable    : ReqTable.State,
                 reqDetail   : ReqDetail.State)

object State {
  def init(cd: ClientData): State =
    State(
      ProjectItem.WithEditableName.State.init,
      "",
      EditorFeature.State.initForProject,
      AsyncFeature.State.initD2,
      PreviewFeature.State.init,
      HideDead,
      ReqTable.State.init(cd, HideDead, None),
      ReqDetail.initState)

  val reqTableVS = State.reqTable ^|-> ReqTable.State.viewSettings
}
