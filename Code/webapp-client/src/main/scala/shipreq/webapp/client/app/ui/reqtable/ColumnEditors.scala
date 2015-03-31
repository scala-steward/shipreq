package shipreq.webapp.client.app.ui.reqtable

import scalaz.Memo
import scalaz.effect.IO
import shipreq.base.util.ScalaExt._
import shipreq.base.util.Rx
import shipreq.webapp.base.data._

final class ColumnEditors(project: Rx[Project],
                          cellset: Cell.SetIO) {

  type SetLocal = Option[Cell.State] => IO[Unit]

  type ColStartEdit = (Row, SetLocal) => Option[Cell.State]

  def startCellEditing(row: Row, col: Column): Option[IO[Unit]] = {
    val f: ColStartEdit =
      col match {
        case Column.Tags  => startEditingTags
        case Column.Pubid => noEdit
        case Column.CustomField(f) =>
          f match {
            // case id: CustomField.Text       .Id => cfText(id)
            case id: CustomField.Tag        .Id => startEditingCustomTags(id)
            // case id: CustomField.Implication.Id => imps(Row._cfImps ^|-? index(id))
          }
      }

    val setLocal: SetLocal =
      s => cellset(Cell.SetCmd(row.id, col, s))

    val startState = f(row, setLocal)

    startState.map(_ => setLocal(startState))
  }

  val noEdit: ColStartEdit =
    (_, _) => None

  lazy val startEditingTags: ColStartEdit = {
    val lookup = TagEditor.lookupForNoCol(project)
    (row, setLocal) => {
      val initial = row.fold(_.mv.tags)
      TagEditor(initial, project.value(), lookup, setLocal).some
    }
  }

  val startEditingCustomTags: CustomField.Tag.Id => ColStartEdit =
    Memo.mutableHashMapMemo { id =>
      val lookup = TagEditor.lookupForCol(project, id)
      (row, setLocal) => {
        val initial = row.fold(_.exp.cfTags.getOrElse(id, Vector.empty))
        TagEditor(initial, project.value(), lookup, setLocal).some
      }
    }
}
