package shipreq.webapp.client.project.app.root

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import shipreq.webapp.client.base.ui.{BaseStyles, ProjectItem}

object ProjectHome {

  final case class Props(item : ProjectItem.Props,
                         index: ProjectIndex.Props) {
    @inline def render = Component(this)
  }

  final class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement =
      <.main(BaseStyles.maxWidthContainer,
        ProjectItem.render(p.item),
        ProjectIndex.Component(p.index))
  }

  val Component = ReactComponentB[Props]("Home")
    .renderBackend[Backend]
    .build
}
