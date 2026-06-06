package shipreq.webapp.client.project.app.pages.admin.status

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.base.util._
import shipreq.webapp.base.feature.AsyncFeature
import shipreq.webapp.base.protocol.ServerSideProcInvoker
import shipreq.webapp.base.ui.semantic.{Button, Colour, Header, Icon, Segment}
import shipreq.webapp.client.project.app.Style.{statusPage => *}
import shipreq.webapp.member.project.protocol.websocket.UpdateLivenessCmd

object RestoreProjectSegment {

  final case class Props(editability      : Permission,
                         sspUpdateLiveness: ServerSideProcInvoker[UpdateLivenessCmd.Restore.type, ErrorMsg, Any],
                         async            : AsyncFeature.ReadWrite.D0[ErrorMsg]) {
    @inline def render: VdomElement = Component(this)
  }

  private def render(p: Props) = {

    val inFlight: Boolean =
      p.async.isInProgress

    val onClick =
      p.async.write.onFailureShowAndForget(p.sspUpdateLiveness(UpdateLivenessCmd.Restore))

    Segment.tag(*.segment,
      <.div(*.segmentLeft,
        Header.h4("Restore"),
        <.p("Unmark this project as deleted and reinstate it."),
      ),
      <.div(
        Button(
          tipe   = Button.Type.IconAndText(Icon.Undo, "Restore"),
          state  = Button.State.loadingOrEnabled(loading = inFlight, enabled = p.editability is Allow),
          colour = Colour.Green,
        ).tag(^.onClick --> onClick, *.segmentButton)
      )
    )
  }

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .build
}
