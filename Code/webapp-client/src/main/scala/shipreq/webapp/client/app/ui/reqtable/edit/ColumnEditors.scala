package shipreq.webapp.client.app.ui.reqtable
package edit

import japgolly.scalajs.react.extra.Px
import monocle.Optional
import scalaz.effect.IO
import scalaz.syntax.equal._
import shipreq.base.util.ScalaExt._
import shipreq.base.util.Must
import shipreq.webapp.base.data._
import shipreq.webapp.base.text.{TextSearch, PlainText}
import shipreq.webapp.client.app.ui.ProjectWidgets
import DataImplicits._

final class ColumnEditors(project       : Px[Project],
                          plainText     : Px[PlainText.ForProject],
                          projectWidgets: Px[ProjectWidgets],
                          textSearch    : Px[TextSearch],
                          cellset       : Cell.SetIO) {

  type State                = Option[Cell.State]
  type SetState             = State => IO[Unit]
  type InitState            = SetState => State
  type InitEditor[R <: Row] = R => InitState

  private def noEditor: InitState =
    _ => None

  @inline private implicit def autoSome(c: Cell.State): Option[Cell.State] =
    Some(c)

  @inline private def initEditor[R <: Row](f: R => SetState => Option[Cell.State]): InitEditor[R] =
    f

  private def initEditorO[R <: Row](f: R => Option[SetState => Cell.State]): InitEditor[R] =
    r => f(r).fold[SetState => Option[Cell.State]](_ => None)(g => g(_).some)

  private val applicability = project.map(Applicability.apply)

  def startCellEditing(row: Row, col: Column): Option[IO[Unit]] =
    row.alive match {
      case Alive => startCellEditing2(row, col)
      case Dead  => None
    }

  private def startCellEditing2(row: Row, col: Column): Option[IO[Unit]] = {
    val init: InitState =
      applicability.value().apply(col).choose(row, na = noEditor)(
        row match {

          case r: GenericReqRow =>
            col match {
              case Column.Code           => codesForReq(r)
              case Column.Title          => genericReqTitle(r)
              case Column.Tags           => tags(r)
              case Column.ReqType        => reqType(r)
              case Column.Pubid          => noEditor
              case Column.ImplicationSrc => imps(Row.implicationSrc, col)(r)
              case Column.ImplicationTgt => imps(Row.implicationTgt, col)(r)
              case Column.CustomField(f, _) =>
                f match {
                  case id: CustomField.Text       .Id => cfText(id)(r)
                  case id: CustomField.Tag        .Id => cfTag(id)(r)
                  case id: CustomField.Implication.Id => cfImp(id, col)(r)
                }
            }

          case r: ReqCodeGroupRow =>
            col match {
              case Column.Code           => codesForGroup(r)
              case Column.Title          => reqCodeGroupTitle(r)
              case Column.Pubid
                 | Column.ReqType
                 | Column.Tags
                 | Column.ImplicationSrc
                 | Column.ImplicationTgt
                 | _: Column.CustomField => noEditor
            }
        }
      )

    val setState: SetState =
      s => cellset(Cell.SetCmd(row.id, col, s))

    val initialState: State =
      init(setState)

    initialState.map(_ => setState(initialState))
  }

  val reqType = initEditorO[GenericReqRow] { r =>
    val initialM = project.value().reqTypeC(r.req.reqTypeId)
    mustResolveO(initialM).map { initial =>
      val fields = project.map(_.customReqTypes.data.values.toSet)
      ReqTypeSelector(initial, fields, _)
    }
  }

  val reqCodeGroupTitle = initEditor[ReqCodeGroupRow](r =>
    RichTextEditor.ReqCodeGroupTitle(r.group.title, project, plainText, projectWidgets, textSearch, _))

  val genericReqTitle = initEditor[GenericReqRow](r =>
    RichTextEditor.GenericReqTitle(r.req.title, project, plainText, projectWidgets, textSearch, _))

  val codesForGroup = initEditor[ReqCodeGroupRow] { r =>
    val currentValue = r.reqCode
    val vs = project.map(p => Validators.reqCode.VS(p.reqCodes.data.trie, Set(currentValue)))
    ReqCodeEditor.ForGroup(currentValue, vs, _)
  }

  val codesForReq = initEditor[GenericReqRow] { r =>
    val currentValues = project.value().reqCodes.data.activeReqCodesByTarget(r.req.id)
    val vs = project.map(p => Validators.reqCode.VS(p.reqCodes.data.trie, currentValues))
    ReqCodeEditor.ForReqs(currentValues, vs, _)
  }

  val tags = initEditor[GenericReqRow] { r =>
    val lookup = project map TagEditor.lookupForNoCol
    TagEditor(r.mv.tags.toSet, project.value(), lookup, _)
  }

  def cfTag(id: CustomField.Tag.Id) = initEditor[GenericReqRow] { r =>
    val lookup = project map (TagEditor.lookupForCol(_, id))
    TagEditor(r.exp.tagsForCF(id).toSet, project.value(), lookup, _)
  }

  lazy val impLookup =
    Px.apply2(project, plainText)(ImplicationEditor.lookupAll)

  def imps(l: Optional[Row, Vector[Pubid]], col: Column) = initEditorO[GenericReqRow](r =>
    l.getOption(r).map(initialValue =>
      ImplicationEditor(initialValue, r.req.id, col, project, textSearch, impLookup map Must.apply, _)))

  def cfImp(fid: CustomField.Implication.Id, col: Column) = initEditorO[GenericReqRow] { r =>
    val lookup = for {p <- project; l <- impLookup} yield ImplicationEditor.lookupForCustomImpCol(p, l, fid)
    Row.cfImp(fid).getOption(r).map { _ =>
      val id = r.req.id
      val initial = ImplicationEditor.initialValueForCustomColumn(project.value(), fid, id)
      ImplicationEditor(initial, id, col, project, textSearch, lookup, _)
    }
  }

  def cfText(id: CustomField.Text.Id) = initEditor[GenericReqRow] { r =>
    val textData = project.value().reqFieldData.data.text.getOrElse(id, Map.empty)
    val initialValue = textData.get(r.req.id).map(_.whole) getOrElse Vector.empty
    RichTextEditor.CustomTextField(initialValue, project, plainText, projectWidgets, textSearch, _)
  }
}
