package shipreq.webapp.client.loaders

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import shipreq.webapp.base.config.{AssetManifest, ClientConfig}
import shipreq.webapp.base.data.Username
import shipreq.webapp.base.ui.semantic.Breadcrumb
import shipreq.webapp.member.ui._

object HomeSpaLoader {

  final case class Props(username: Username, am: AssetManifest) {
    @inline def render: VdomElement = Component(this)
  }

  private val navBarLeft: MemberNavBar.LeftProps =
    Reusable.byRef(Breadcrumb.Item.Section(ClientConfig.BreadcrumbNameMemberHome) :: Nil)

  private def render(p: Props): VdomElement = {
    val navBar = MemberNavBar.Props(
      username      = p.username,
      am            = p.am,
      feedbackModal = None,
      left          = navBarLeft)
    Loader.render(navBar)(EmptyVdom)
  }

  val Component = ScalaFnComponent[Props](render)
}
