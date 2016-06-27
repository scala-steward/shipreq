package shipreq.webapp.client.project.app.root

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._
import shipreq.webapp.client.base.ui.{BaseStyles, ProjectItem}
import shipreq.webapp.client.project.app.Style

object ProjectHome {

  final case class Props(item : ProjectItem.WithEditableName.Props,
                         index: ProjectIndex.Props) {
    @inline def render = Component(this)
  }

  final class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement =
      <.main(BaseStyles.containerLarge,
        <.section(Style.home.projectHeader, p.item.render),
        ProjectIndex.Component(p.index))
  }

  val Component = ReactComponentB[Props]("Home")
    .renderBackend[Backend]
    .build
}
