package shipreq.webapp.client.app.ui.reqtable

import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import org.scalajs.dom
import shipreq.base.util.Must
import shipreq.base.util.ScalaExt._
import shipreq.webapp.base.data._
import shipreq.webapp.client.app.ui.widget._
import SCRATCH._
import DataImplicits._

object Table {

  case class Props(viewSettings: ViewSettings,
                   project     : Project,
                   columnName  : Column.NameResolver)

  val Component =
    ReactComponentB[Props]("Table")
      .stateless
      .backend(new Backend(_))
      .render(_.backend.render)
      .build

  val noReqCodesToExpand: List[List[ReqCode]] = List(Nil)

  final class Backend($: BackendScope[Props, Unit]) {
    def render: ReactElement = {
      val p = $.props

      // Init columns
      val xxx = ColumnRenderer.thingy(p.project, p.columnName)
      val crs: Vector[ColumnRenderer] =
        p.viewSettings.columns.map(xxx)

      val expandCodes = p.viewSettings.order.includesCode

      // Collect rows
      var rows: Vector[Row] =
        p.project.reqs.values.foldLeft(Vector.empty[Row])((q, i) => i match {
          case r: GenericReq =>

          // Filter deleted

          // Expansion
          val expandedReqCodes: List[List[ReqCode]] = {
            val codes = p.project.reqCodesPerTarget(r.id)
            if (codes.isEmpty)
              noReqCodesToExpand
            else if (expandCodes)
              codes.foldLeft[List[List[ReqCode]]](Nil)((q2, c) => (c :: Nil) :: q2)
            else
              List(codes.toList) // TODO sort
          }

          (q /: expandedReqCodes)((q2, codes) => q2 :+ GenericReqRow(r, Expansion(None, None, codes)))
        })

      // Add SHRs

      // Sort
      
      // Render
      // TODO handle zero rows nicely. "33 reqs (SHRs?), 11 deleted, 3 excluded by filter."
      <.table(
        <.thead(
          <.tr(
            crs.map(cr =>
              <.th(
                cr.header)))),
        <.tbody(
          rows.map(r =>
            <.tr(
              crs.map(cr =>
                <.td(
                  cr render r))))))
    }
  }
}
