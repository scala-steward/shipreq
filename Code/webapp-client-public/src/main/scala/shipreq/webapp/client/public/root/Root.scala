package shipreq.webapp.client.public.root

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra._
import shipreq.webapp.client.base.protocol.ClientProtocol

object Root {
  final case class Props(page: Page, routerCtl: RouterCtl)
}

final class Root(cp: ClientProtocol) {
  import Root._

  val Component = ScalaComponent.builder[Props]("Root")
    .initialState(State.init)
    .renderBackend[Backend]
    .build

  final class Backend($: BackendScope[Props, State]) {
    def render(p: Props, s: State): VdomElement =
      Layout.Component(Layout.Props(p.page, p.routerCtl))
  }

}