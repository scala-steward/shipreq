package shipreq.webapp.client.project.app.pages.admin.status

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.webapp.base.data.Rolodex
import shipreq.webapp.client.project.app.Style.{statusPage => *}
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.ui.BaseStyles

object StatusPage {

  final case class Props(project: Project,
                         rolodex: Rolodex,
                         meta   : ProjectMetaData) {
    @inline def render: VdomElement = Component(this)
  }

  private def render(p: Props) = {
    <.main(BaseStyles.containerLarge,
      *.main,

      StatusTable.Props(
        project = p.project,
        rolodex = p.rolodex,
        meta    = p.meta,
      ).render,
    )
  }

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .build
}
