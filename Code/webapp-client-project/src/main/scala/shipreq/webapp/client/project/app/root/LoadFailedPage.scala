package shipreq.webapp.client.project.app.root

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import shipreq.webapp.client.base.ui.semantic.{Icon, Message}

object LoadFailedPage {

  final case class Props(lp: LoadingPage.Props, error: String)

  def render(p: Props): ReactElement = {
    val msg = Message(
      Message.Style(Message.Type.Error),
      Icon.WarningCircle,
      "Error loading project",
      p.error)

    LoadingPage.layout(p.lp)(
      <.div(^.paddingTop := "4rem", msg))
  }

  val Component = FunctionalComponent[Props](render)
}
