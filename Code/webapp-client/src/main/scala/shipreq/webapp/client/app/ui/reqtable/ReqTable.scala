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

  def initialState(p: Project): State =
    new State(p)

  class State(initialProject: Project) {
    var project     : Project             = initialProject
    var colName     : Column.NameResolver = Column.NameResolver(project.fields.data.customFields, CustomField nameP project)
    var viewSettings: ViewSettings        = ViewSettings.default
    var content     : Table.Content       = Table.content(viewSettings, project, colName)
    var focus       : Option[Table.Focus] = None

    def setViewSettings(newVS: ViewSettings): State = {
      viewSettings = newVS
      content      = Table.content(newVS, project, colName)
      focus        = focus // TODO
      this
    }

    def setFocus(f: Option[Table.Focus]): State = {
      focus = f
      this
    }

    var viewSettingsEditor = ViewSettingsEditor(colName)
  }

  // TODO modStateR can be in util
  def modStateR[S, A]($: BackendScope[_, S])(f: S => A => S): A ~=> IO[Unit] =
    ReusableFn(a => $.modStateIO(s => f(s)(a)))

  final class Backend($: BackendScope[Project, State]) {

    val setViewSettings = modStateR($)(_.setViewSettings)
    val setFocus        = modStateR($)(_.setFocus)

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
