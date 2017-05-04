package shipreq.webapp.client.project.app.reqtable2

import japgolly.microlibs.nonempty.NonEmptyVector
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import scalacss.ScalaCssReact._
import shipreq.base.util.ScalaExt.EndoFn
import shipreq.webapp.base.data._
import shipreq.webapp.client.base.lib.DomUtil._
import shipreq.webapp.client.base.ui.{EditTheme, semantic}
import shipreq.webapp.client.project.app.Style.reqtable2.{table => *}
import shipreq.webapp.client.project.feature.EditorFeature
import shipreq.webapp.client.project.widgets.DragToReorder

object Table {

  object Whole {

    final case class Props(rows       : Vector[Row],
                           cols       : NonEmptyVector[ColumnPlus],
                           selection  : RowSelectionVisible,
                           editor     : EditorFeature.ReadWrite.ForProject,
                           modSettings: ModFn[TableSettings]) {
      @inline def render = Component(this)
    }

    implicit val reusabilityProps: Reusability[Props] =
      Reusability.caseClass

    def render(p: Props): VdomElement = {
//      val crs  = p.colRenderers
//      val rows = p.rows

      val header =
        Header.Component(
          Header.Props(
            p.cols,
            p.selection,
            p.modSettings.map(f => cs => f(_ setColumns cs.map(_.column))),
            p.modSettings.map(f => c => f(TableSettings.order.modify(_ want c.column)))))

//      val renderRows =
//        rows.indices.toVdomArray { i =>
//          rows(i) match {
//            case row: ReqRow =>
//              import ForRowReq._
//              val rp = RowProps(row, crs, p.editor.forReq(row.req.id), p.asyncState(row.sourceId), p.selection)
//              RowComponent.withKey(row.id.key)(rp)
//            case row: ReqCodeGroupRow =>
//              import ForRowReqCodeGroup._
//              val rp = RowProps(row, crs, p.editor.forReqCodeGroup(row.reqCodeId), p.selection)
//              RowComponent.withKey(row.id.key)(rp)
//          }
//        }

      // Render
      semantic.Table.celledCompactUnstackable(
        header,
        <.tbody)
//        <.tbody(renderRows))

    }

    val Component = ScalaComponent.builder[Props]("ReqTable")
      .render_P(render)
      .configure(shouldComponentUpdate)
      .build
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  object Header {

    case class Props(cols     : NonEmptyVector[ColumnPlus],
                     selection: RowSelectionVisible,
                     reorder  : NonEmptyVector[ColumnPlus] ~=> Callback,
                     clickSort: ColumnPlus ~=> Callback)

    implicit val reusabilityProps = Reusability.caseClass[Props]

    final class Backend($: BackendScope[Props, Unit]) {

      private def setNewOrder(newOrder: Vector[ColumnPlus]): Callback =
        NonEmptyVector.maybe(newOrder, Callback.empty)(newCols =>
          $.props.flatMap(_ reorder newCols))

      private def selColKeyDown(e: ReactKeyboardEventFromHtml): Callback =
        focusKeyHandlers(e)

      private def dataColKeyDown(col: ColumnPlus)(e: ReactKeyboardEventFromHtml): Callback =
        focusKeyHandlers(e) | keyCodeSwitch(e) {
          case KeyCode.Space => $.props.flatMap(_ clickSort col)
        }

      private def renderFn(p: Props, content: DragToReorder.Content[ColumnPlus]): VdomElement = {
        val selectionCell =
          <.th(
            *.selectionColumnHeader,
            ^.onKeyDown ==> selColKeyDown,
            p.selection.total.checkboxAndOnClick)

        val cols =
          content.items.toVdomArray { i =>
            val c = i.data
            val live = c.column match {
              case Column.DeletionReason => Live // Don't render this title with strike-through
              case _                     => c.live
            }
            <.th(
              *.header(live, i.status),
              i.mod,
              ^.tabIndex   := -1,
              ^.onKeyDown ==> dataColKeyDown(c),
              ^.onClick   --> p.clickSort(c),
              c.name)
          }

        <.thead(
          content.rootMod,
          <.tr(
            selectionCell,
            cols))
      }

      private val columnDND: DragToReorder[ColumnPlus] =
        new DragToReorder(setNewOrder, c => $.props.map(renderFn(_, c)))

      def render(p: Props): VdomElement =
        columnDND.Component(p.cols.whole)
    }

    val Component = ScalaComponent.builder[Props]("Header")
      .renderBackend[Backend]
      .configure(shouldComponentUpdate)
      .build
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████
/*
  object ReqRow {

    final case class Props(rows: Vector[Row]) {
      @inline def render = Component(this)
    }

    // implicit val reusabilityProps: Reusability[Props] =
    //   Reusability.caseClass

    final class Backend($: BackendScope[Props, Unit]) {

      def render(p: Props): VdomElement =
        <.div
    }

    val Component = ScalaComponent.builder[Props]("ReqTable")
      .renderBackend[Backend]
      // .configure(Reusability.shouldComponentUpdate)
      .build
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  object ReqCodeGroupRow {

    final case class Props(rows: Vector[Row]) {
      @inline def render = Component(this)
    }

    // implicit val reusabilityProps: Reusability[Props] =
    //   Reusability.caseClass

    final class Backend($: BackendScope[Props, Unit]) {

      def render(p: Props): VdomElement =
        <.div
    }

    val Component = ScalaComponent.builder[Props]("ReqTable")
      .renderBackend[Backend]
      // .configure(Reusability.shouldComponentUpdate)
      .build
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  object Cell {

    final case class Props(rows: Vector[Row]) {
      @inline def render = Component(this)
    }

    // implicit val reusabilityProps: Reusability[Props] =
    //   Reusability.caseClass

    final class Backend($: BackendScope[Props, Unit]) {

      def render(p: Props): VdomElement =
        <.div
    }

    val Component = ScalaComponent.builder[Props]("ReqTable")
      .renderBackend[Backend]
      // .configure(Reusability.shouldComponentUpdate)
      .build
  }
*/

  // Shared fns

  def moveFocus(cur: dom.html.Element, ↔ : Movement = Movement.None, ↕ : Movement = Movement.None): Callback =
    Callback {
      val cell: dom.html.Element =
        if ("INPUT" == cur.tagName) // Selection checkbox
          cur.parentElement
        else
          cur
      val z = TableCellZipper(cell) move_- ↔ move_| ↕
      val f: dom.html.Element =
        if (z.colIndex == 0)
          z.focus.children(0).domAsHtml // Selection checkbox
        else
          z.focus
      f.focus()
    }

  def focusKeyHandlers(e: ReactKeyboardEventFromHtml): CallbackOption[Unit] =
    keyCodeSwitch(e) {
      case KeyCode.Up     => moveFocus(e.currentTarget, ↕ = Movement.Prev)
      case KeyCode.Down   => moveFocus(e.currentTarget, ↕ = Movement.Next)
      case KeyCode.Left   => moveFocus(e.currentTarget, ↔ = Movement.Prev)
      case KeyCode.Right  => moveFocus(e.currentTarget, ↔ = Movement.Next)
      case KeyCode.Home   => moveFocus(e.currentTarget, ↔ = Movement.Head)
      case KeyCode.End    => moveFocus(e.currentTarget, ↔ = Movement.Last)
      case KeyCode.Escape => Callback(e.target.blur())
    } | keyCodeSwitch(e, ctrlKey = true) {
      case KeyCode.Home   => moveFocus(e.currentTarget, Movement.Head, Movement.Head)
      case KeyCode.End    => moveFocus(e.currentTarget, Movement.Last, Movement.Last)
    }
}
