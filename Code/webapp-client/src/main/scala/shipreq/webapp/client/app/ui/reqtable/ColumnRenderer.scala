package shipreq.webapp.client.app.ui.reqtable

import monocle.Optional
import monocle.function.index
import monocle.std.mapIndex
import japgolly.scalacss.ScalaCssReact._
import japgolly.scalacss.StyleA
import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import shipreq.base.util.Must
import shipreq.base.util.ScalaExt._
import shipreq.webapp.base.data._
import shipreq.webapp.client.app.ui.ProjectWidgets
import shipreq.webapp.client.app.ui.Style.{reqtable => *}
import DataImplicits._


final class ColumnRenderer(
  val header     : ReactElement,
  val render     : Row => ReactElement,
  val columnStyle: Option[StyleA])

object ColumnRenderer {
  val `N/A`: ReactElement =
    <.span(*.`N/A`, "–")
}

class ColumnRenderers(project: Project, columnName: Column.NameResolver, widgets: ProjectWidgets) {

  def apply(c: Column): ColumnRenderer = c match {
    case Column.PubId          => pubId(c)
    case Column.ReqType        => reqType(c)
    case Column.Code           => code(c)
    case Column.Desc           => desc(c)
    case Column.Tags           => tags(c)
    case Column.ImplicationSrc => imps(Row._implicationSrc)(c) //("… ⇒")
    case Column.ImplicationTgt => imps(Row._implicationTgt)(c) //("⇒ …")
    case Column.CustomField(f) =>
      f match {
        case id: CustomField.Text       .Id => cfText(id)(c)
        case id: CustomField.Tag        .Id => cfTags(id)(c)
        case id: CustomField.Implication.Id => imps(Row._cfImps ^|-? index(id))(c)
      }
  }

  protected def makeSY(columnStyle: Option[StyleA])(render: Row => ReactElement): String => ColumnRenderer =
    s => new ColumnRenderer(<.span(s), render, columnStyle)

  protected def make(render: Row => ReactElement): Column => ColumnRenderer =
    c => makeSY(None)(render)(columnName(c))

  protected def makeS(render: Row => ReactElement): String => ColumnRenderer =
    makeSY(None)(render)

  // @deprecated("placeholder is for dev purposes only.", "")
  def placeholder =
    new ColumnRenderer(<.span("∅"), Function const <.span("∅"), None)

  def pubId = make {
    case GenericReqRow(req, _, _) => widgets.pubIdText(req.pubId)()
  }

  def reqType = make {
    case GenericReqRow(req, _, _) => widgets.reqType(req.reqTypeId)()
  }

  def code = {
    def render(codes: List[ReqCode]): ReactElement =
          <.ul(codes.map(c => <.li(c.txt)))
    make {
      case GenericReqRow(_, exp, _) => render(exp.reqCodes)
    }
  }

  def tags = make {
    case GenericReqRow(_, _, mv) => widgets.tagList(mv.tags)
  }

  def cfTags(id: CustomField.Tag.Id) = make {
    case GenericReqRow(_, exp, _) => widgets.tagList(exp.cfTags.getOrElse(id, Nil))
  }

  def desc = make {
    case GenericReqRow(req, _, _) => widgets.text(req.desc)
  }

  val empty: ReactElement = <.span

  def cfText(id: CustomField.Text.Id) = {
    val textData = project.reqFieldData.data.text.getOrElse(id, Map.empty)
    make {
      case GenericReqRow(req, _, _) => textData.get(req.id) map (widgets.text1(_)) getOrElse empty
    }
  }
  
  def imps(l: Optional[Row, List[Pubid]]) = make(
    l.getMaybe(_).cata(widgets.pubidRefList, empty))
}
