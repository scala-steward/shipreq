package shipreq.webapp.client.app.ui.reqtable

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
      }

    val setLocal: SetLocal =
      s => cellset(Cell.SetCmd(row.id, col, s))

    val startState = f(row, setLocal)

    startState.map(_ => setLocal(startState))
  }

  val noEdit: ColStartEdit =
    (_, _) => None

  lazy val tagLookup: Rx[TagEditor.Lookup] =
    TagEditor.lookupX(project)

  val startEditingTags: ColStartEdit = (row, setLocal) => {
    val initial = (row match {case y: GenericReqRow => y}).mv.tags
    TagEditor(initial, project.value(), tagLookup, setLocal).some
  }

}
