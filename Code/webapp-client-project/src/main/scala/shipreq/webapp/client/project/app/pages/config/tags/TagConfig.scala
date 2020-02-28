package shipreq.webapp.client.project.app.pages.config.tags

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import shipreq.webapp.base.data.TagId
import shipreq.webapp.base.lib.DataReusability._
import shipreq.webapp.client.project.widgets.SplitScreenCrud

object TagConfig {

  type NewState = Unit
  type EditorState = Unit

  val splitScreenCrud = new SplitScreenCrud[NewState, TagId, EditorState](
    initEditor = _ => ???, //: Id => CallbackTo[EditorState],
    rightEmpty = SplitScreenCrud.emptyEditorMessage("tag"),
  )

  final case class Props(state: StateSnapshot[State]) {
    @inline def render: VdomElement = Component(this)
  }

  type State = splitScreenCrud.State

  def initState: State =
    splitScreenCrud.initState(())

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomNode = {
      splitScreenCrud(
        newButton = _ => <.div("todo"), // NewArgs[NewState] => VdomNode,
        list      = _ => <.div("todo"), // ListArgs[Id] => VdomNode,
        editor    = _ => <.div("todo"), // EditorArgs[Id, EditorState] => VdomNode,
        state     = p.state,
      )
    }
  }

  val Component = ScalaComponent.builder[Props]("TagConfig")
    .renderBackend[Backend]
    .build
}