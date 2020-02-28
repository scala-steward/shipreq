package shipreq.webapp.client.project.app.pages.config.fields

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object FieldConfig {

  final case class Props() {
    @inline def render: VdomElement = Component(this)
  }

  //implicit val reusabilityProps: Reusability[Props] =
  //  Reusability.derive

  final class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomNode =
      <.div("WIP")
  }

  val Component = ScalaComponent.builder[Props]("FieldConfig")
    .renderBackend[Backend]
    //.configure(Reusability.shouldComponentUpdate)
    .build
}