package shipreq.webapp.client.project.app.reqdetail

import japgolly.scalajs.react.extra.Reusability
import shipreq.base.util._
import shipreq.base.util.univeq._
import shipreq.webapp.base.data.{CustomFieldId, UseCaseStepId}
import shipreq.webapp.client.project.feature.ContentEditorFeature.EditFieldKey
import shipreq.webapp.client.project.lib.DataReusability._

sealed abstract class Cell

object Cell {
  case object ReqType                                  extends Cell
  case object Code                                     extends Cell
  case object Title                                    extends Cell
  case object Tags                                     extends Cell
  case class Implications      (dir: Direction)        extends Cell
  case class CustomField       (id: CustomFieldId)     extends Cell
  case class UseCaseStep       (id: UseCaseStepId)     extends Cell
  case class UseCaseStepCtrls  (id: UseCaseStepId)     extends Cell
  case class AddUseCaseStep    (id: UseCaseStepId)     extends Cell
  case class AddUseCaseTailStep(row: Row.UseCaseSteps) extends Cell

  object Implications {
    private val memo = Direction.memo(new Implications(_))
    def apply(d: Direction): Implications = memo(d)
  }

  @inline implicit def univEq: UnivEq[Cell] =
    UnivEq.derive

  implicit val reusability: Reusability[Cell] =
    Reusability.byUnivEq

  val EditFieldKeyIntersection = Intersection[Cell, EditFieldKey] {
    case Cell.ReqType               => Some(EditFieldKey.ReqType)
    case Cell.Code                  => Some(EditFieldKey.Code)
    case Cell.Title                 => Some(EditFieldKey.Title)
    case Cell.Tags                  => Some(EditFieldKey.Tags)
    case Cell.Implications(dir)     => Some(EditFieldKey.Implications(dir))
    case Cell.CustomField(id)       => Some(EditFieldKey.CustomField(id))
    case Cell.UseCaseStep(id)       => Some(EditFieldKey.UseCaseStep(id))
    case Cell.UseCaseStepCtrls(_)
       | Cell.AddUseCaseStep(_)
       | Cell.AddUseCaseTailStep(_) => None
  } {
    case EditFieldKey.ReqType           => Some(Cell.ReqType)
    case EditFieldKey.Code              => Some(Cell.Code)
    case EditFieldKey.Title             => Some(Cell.Title)
    case EditFieldKey.Tags              => Some(Cell.Tags)
    case EditFieldKey.Implications(dir) => Some(Cell.Implications(dir))
    case EditFieldKey.CustomField(id)   => Some(Cell.CustomField(id))
    case EditFieldKey.UseCaseStep(id)   => Some(Cell.UseCaseStep(id))
  }
}