package shipreq.webapp.client.app.ui.reqtable

import monocle.Optional
import scalaz.effect.IO
import shipreq.base.util.ScalaExt._
import shipreq.base.util.{Must, Px}
import shipreq.base.util.UnivEq.{mutableHashMapMemo => memo}
import shipreq.webapp.base.data._
import shipreq.webapp.base.text.{TextSearch, PlainText}
import shipreq.webapp.client.app.ui.ProjectWidgets

final class ColumnEditors(project       : Px[Project],
                          plainText     : Px[PlainText.ForProject],
                          projectWidgets: Px[ProjectWidgets],
                          textSearch    : Px[TextSearch],
                          cellset       : Cell.SetIO) {

  type SetLocal = Option[Cell.State] => IO[Unit]

  type ColStartEdit = (Row, SetLocal) => Option[Cell.State]

  def startCellEditing(row: Row, col: Column): Option[IO[Unit]] = {
    val e: ColStartEdit =
      col match {
        case Column.Title          => title
        case Column.Tags           => tags
        case Column.Pubid          => noEdit
        case Column.ImplicationSrc => imps(Row.implicationSrc, ImplicationEditor declFwd Column.ImplicationSrc)
        case Column.ImplicationTgt => imps(Row.implicationTgt, ImplicationEditor declFwd Column.ImplicationTgt)
        case Column.CustomField(f) =>
          f match {
            case id: CustomField.Text       .Id => cfText.value()(id)
            case id: CustomField.Tag        .Id => cfTag(id)
            case id: CustomField.Implication.Id => cfImp(id)
          }
      }

    val setLocal: SetLocal =
      s => cellset(Cell.SetCmd(row.id, col, s))

    val startState = e(row, setLocal)

    startState.map(_ => setLocal(startState))
  }

  val noEdit: ColStartEdit =
    (_, _) => None

  lazy val title: ColStartEdit = {
    //val lookup = project map TagEditor.lookupForNoCol
    (row, setLocal) => {
      val initialValue = row.fold(_.fold(_.req.title))
      RichTextEditor.GenericReqTitle(initialValue, project, plainText, projectWidgets, textSearch, setLocal).some
    }
  }

  lazy val tags: ColStartEdit = {
    val lookup = project map TagEditor.lookupForNoCol
    (row, setLocal) => {
      val initialValue = row.fold(_.mv.tags)
      TagEditor(initialValue, project.value(), lookup, setLocal).some
    }
  }

  val cfTag: CustomField.Tag.Id => ColStartEdit =
    memo { id =>
      val lookup = project map (TagEditor.lookupForCol(_, id))
      (row, setLocal) => {
        val initialValue = row.fold(_.exp.cfTags.getOrElse(id, Vector.empty))
        TagEditor(initialValue, project.value(), lookup, setLocal).some
      }
    }

  lazy val impsLookup =
    Px.apply2(project, plainText)(ImplicationEditor.lookupAll)

  def imps(l: Optional[Row, Vector[Pubid]], declFwd : Boolean): ColStartEdit = (row, setLocal) =>
    l.getOption(row).map { initialValue =>
      val lookup2 = for {p <- project; l <- impsLookup}
        yield Must(ImplicationEditor.lookupForSubject(p, l, row.id, declFwd))
      ImplicationEditor(initialValue, project, textSearch, lookup2, setLocal)
    }

  val cfImp: CustomField.Implication.Id => ColStartEdit =
    memo { id =>
      val lookup2 = for {p <- project; l <- impsLookup} yield ImplicationEditor.lookupForCol(p, l, id)
      (row, setLocal) =>
        Row.cfImp(id).getOption(row).map { initialValue =>
          val declFwd = ImplicationEditor declFwd id
          val lookup3 = for {p <- project; lm <- lookup2}
            yield lm.map(ImplicationEditor.lookupForSubject(p, _, row.id, declFwd))
          ImplicationEditor(initialValue, project, textSearch, lookup3, setLocal)
        }
    }

  val cfText: Px[CustomField.Text.Id => ColStartEdit] =
    project.map(p =>
      memo { id =>
        val textData = p.reqFieldData.data.text.getOrElse(id, Map.empty)
        (row, setLocal) => {
          val initialValue = textData.get(row.id).map(_.whole) getOrElse Vector.empty
          RichTextEditor.CustomTextField(initialValue, project, plainText, projectWidgets, textSearch, setLocal).some
        }
      }
    )
}
