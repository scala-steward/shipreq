package shipreq.webapp.client.project.app.issues

import japgolly.microlibs.nonempty.NonEmptyVector
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra._
import scalacss.ScalaCssReact._
import shipreq.base.util.{ConsolidatedSeq, Util}
import shipreq.webapp.base.UiText
import shipreq.webapp.base.data._
import shipreq.webapp.base.issue._
import shipreq.webapp.base.sort.FusedSorters
import shipreq.webapp.base.ui.semantic
import shipreq.webapp.client.project.app.Style.{issues => *}

object Table {

  final case class Props(project: Project,
                         issues: Issues,
                         fieldNames: FieldId ~=> String) {
    @inline def render: VdomElement = Component(this)
  }

  //implicit val reusabilityProps: Reusability[Props] =
  //  Reusability.caseClass

  private object Internal {


  }

  final class Backend($: BackendScope[Props, Unit]) {
    import Internal._

    private val columns: NonEmptyVector[Column] =
      NonEmptyVector(
        Column.IssueCategory,
        Column.IssueClass,
        Column.Pubid,
        Column.Title,
        Column.FieldName,
        Column.FieldEditor,
        Column.Actions,
      )

    private val td = <.td(*.tableData)

    private val renderGroup: ConsolidatedSeq.Group[String] => VdomElement = g =>
      if (g.size == 1)
        td(g.value)
      else
        td(g.value,
          <.div(*.rowspanOuter, "(", <.span(*.rowspanInner, g.size), ")"),
          ^.rowSpan := g.size)

    private val consolidator = ConsolidatedSeq.Logic.consolidateByUnivEq[String].apply(renderGroup)

    def render(p: Props): VdomElement = {

      val sorter = new FusedSorters(
        Vector(Sorter.issueCategorySorter, Sorter.issueClassSorter),
        Sorter.pubidSorter)
      val setup = new Sorter.Setup(p.project)
      val sortFn = sorter.result(setup)

      val toRow = Row.fromIssue(p.project)
      val rows = sortFn(p.issues.vector.iterator.map(toRow)).iterator.toVector

      val cs1 = consolidator.consolidate(rows.iterator.map(_.issueCategoryDesc))
      val cs2 = consolidator.consolidate(rows.iterator.map(_.issueClassDesc))

      val header = TableHeader.Props(columns, p.fieldNames).render

      val tmp = td("TODO")
      val body = rows.indices.toVdomArray { row =>
        // TODO key
        <.tr(
          cs1(row),
          cs2(row),
          tmp, tmp, tmp, tmp, tmp)
      }

      semantic.Table.celledCompactUnstackable(
        *.table,
        header,
        <.tbody(body))
    }
  }

  val Component = ScalaComponent.builder[Props]("Table")
    .renderBackend[Backend]
    //.configure(Reusability.shouldComponentUpdate)
    .build
}
