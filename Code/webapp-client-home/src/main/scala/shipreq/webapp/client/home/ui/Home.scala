package shipreq.webapp.client.home.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._
import shipreq.webapp.base.protocol.InitDataForHomeSpa

object Home {
  type Props = InitDataForHomeSpa

  private def render(p: Props): ReactElement = {

    val menu = TopMenu.Component(p.username)

    val projects = p.projects.items.sortBy(_.name).map(ProjectItem.Component(_))

    <.div(
      menu,
      <.main(Styles.homeContentContainer,
        projects))
  }

  val Component = FunctionalComponent(render)
}
