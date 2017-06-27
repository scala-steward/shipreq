package shipreq.webapp.client.public.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra._

object LandingPage {

  final case class Props() {
    @inline def render = Component(this)
  }

  //implicit val reusabilityProps: Reusability[Props] =
  //  Reusability.caseClass

  final class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement =
      <.div("LANDING PAGE")
  }

  val Component = ScalaComponent.builder[Props]("LandingPage")
    .renderBackend[Backend]
    //.configure(Reusability.shouldComponentUpdate)
    .build
}