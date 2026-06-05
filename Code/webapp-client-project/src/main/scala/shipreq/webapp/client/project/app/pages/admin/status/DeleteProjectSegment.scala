package shipreq.webapp.client.project.app.pages.admin.status

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.base.util._
import shipreq.webapp.base.ui.semantic.{Button, ColourPlus, Header, Icon, Segment}
import shipreq.webapp.client.project.app.Style.{statusPage => *}

object DeleteProjectSegment {

  final case class Props(editability: Permission,
                         onDelete   : Callback) {
    @inline def render: VdomElement = Component(this)
  }

  private def render(p: Props) = {
    Segment.tag(*.segment,
      <.div(*.segmentLeft,
        Header.h4("Delete"),
        <.p("Mark this project as deleted and read-only. It can be restored at any time."),
      ),
      <.div(
        Button(
          tipe   = Button.Type.BasicIconAndText(Icon.Trash, "Delete"),
          state  = Button.State.enabledWhen(p.editability is Allow),
          colour = ColourPlus.Negative,
        ).tag(^.onClick --> p.onDelete, *.segmentButton)
      )
    )
  }

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .build
}
