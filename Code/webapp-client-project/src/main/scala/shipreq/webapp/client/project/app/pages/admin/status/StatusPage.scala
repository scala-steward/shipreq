package shipreq.webapp.client.project.app.pages.admin.status

import japgolly.scalajs.react.ReactMonocle._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import monocle.macros.Lenses
import scalacss.ScalaCssReact._
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.data.Rolodex
import shipreq.webapp.base.feature.AsyncFeature
import shipreq.webapp.base.protocol.ServerSideProcInvoker
import shipreq.webapp.client.project.app.Style.{statusPage => *}
import shipreq.webapp.client.project.widgets.ProjectWidgets
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.protocol.websocket.UpdateLivenessCmd
import shipreq.webapp.member.project.text.TextSearch
import shipreq.webapp.member.ui.BaseStyles

object StatusPage {

  final case class Props(project          : Project,
                         rolodex          : Rolodex,
                         meta             : ProjectMetaData,
                         widgets          : ProjectWidgets.NoCtx,
                         textSearch       : TextSearch,
                         state            : StateSnapshot[State],
                         sspUpdateLiveness: ServerSideProcInvoker[UpdateLivenessCmd, ErrorMsg, Any],
                         async            : AsyncFeature.ReadWrite.D0[ErrorMsg],
                        ) {
    @inline def render: VdomElement = Component(this)
  }

  @Lenses
  final case class State(showDeletionForm: Boolean,
                         deleteForm      : DeleteProjectForm.State)

  object State {
    def init: State =
      State(
        showDeletionForm = false,
        deleteForm       = DeleteProjectForm.State.init)
  }

  private def render(p: Props) = {

    def table = StatusTable.Props(
        project = p.project,
        rolodex = p.rolodex,
        meta    = p.meta,
      ).render

    def deleteSegment = DeleteProjectSegment.Props(
      onDelete = p.state.modState(_.copy(showDeletionForm = true)),
    ).render

    def deleteForm = DeleteProjectForm.Props(
      projectName       = p.project.name,
      project           = p.project,
      widgets           = p.widgets,
      textSearch        = p.textSearch,
      state             = p.state.zoomStateL(State.deleteForm),
      close             = p.state.modState(_.copy(showDeletionForm = false)),
      sspUpdateLiveness = p.sspUpdateLiveness,
      async             = p.async,
    ).render

    <.main(BaseStyles.containerLarge, *.main,
      if (p.state.value.showDeletionForm)
        deleteForm
      else
        <.div(
          table,
          deleteSegment,
        )
    )
  }

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .build
}
