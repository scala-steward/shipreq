package shipreq.webapp.client.project.app.pages.admin.status

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.data.Rolodex
import shipreq.webapp.base.feature.AsyncFeature
import shipreq.webapp.client.project.app.Style.{statusPage => *}
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.ui.BaseStyles

object StatusPage {

  final case class Props(project: Project,
                         rolodex: Rolodex,
                         meta   : ProjectMetaData,
                         state  : StateSnapshot[State],
                         async  : AsyncFeature.ReadWrite.D0[ErrorMsg]) {
    @inline def render: VdomElement = Component(this)
  }

  final case class State(showDeletionForm: Boolean)

  object State {
    def init: State =
      State(showDeletionForm = false)
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
      projectName = p.project.name,
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
