package shipreq.webapp.client.app.ui.reqtable

import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._, MonocleReact._
import scalaz.effect.IO
import shipreq.webapp.base.data._
import shipreq.webapp.client.util._

object ReqTable {

  val WIP =
    ReactComponentB[Project]("WIP")
      .getInitialState(initialState)
      .backend(new Backend(_))
      .render(_.backend.render)
      .build

  def initialState(p: Project): State = {
    val colName = Column.NameResolver(p.fields.data.customFields, CustomField nameP p)
    val vs      = ViewSettings.default
    State(p, colName, vs, None, Table.content(vs, p, colName), ViewSettingsEditor(colName))
  }

  case class State(project           : Project,
                   colName           : Column.NameResolver,
                   viewSettings      : ViewSettings,
                   focus             : Option[Table.Focus],
                   content           : Table.Content,
                   viewSettingsEditor: ViewSettingsEditor.Component) {

    def updateVS(newVS: ViewSettings): State = {
      val newContent = Table.content(newVS, project, colName)
      val newFocus   = focus // TODO
      copy(viewSettings = newVS, content = newContent, focus = newFocus)
    }

    def updateFocus(newFocus: Option[Table.Focus]): State =
      copy(focus = newFocus)
  }

  // TODO modStateR can be in util
  def modStateR[S, A]($: BackendScope[_, S])(f: S => A => S): A ~=> IO[Unit] =
    ReusableFn(a => $.modStateIO(s => f(s)(a)))

  final class Backend($: BackendScope[Project, State]) {

    val setViewSettings = modStateR($)(_.updateVS)
    val setFocus        = modStateR($)(_.updateFocus)

    def render = {
      val S = $.state
      val focusV        = setFocus.extvar(S.focus)
      val viewSettingsV = setViewSettings.extvarR(S.viewSettings, Reusable.byRef)

      <.div(
        S.viewSettingsEditor(viewSettingsV),
        Table.Component(Table.Props(S.project, S.content, focusV)))
    }
  }
}
