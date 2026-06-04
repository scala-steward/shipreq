package shipreq.webapp.client.project.app.pages.admin.status

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.webapp.base.ui.semantic.{Colour, Icon, Message, Size}
import shipreq.webapp.client.project.app.Style.{statusPage => *}

object DeleteProjectForm {

  final case class Props(projectName: String) {
    @inline def render: VdomElement = Component(this)
  }

  private def render(p: Props) = {

    val heading = Message(
      Message.Style(colour = Colour.Black, size = Size.Huge),
      Icon.Trash,
      "You are about to delete the project:",
      <.div(*.deleteFormHeaderProjectName, p.projectName)
    )

    heading
  }

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .build
}
