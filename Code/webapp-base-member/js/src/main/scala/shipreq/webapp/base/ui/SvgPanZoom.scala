package shipreq.webapp.base.ui

import japgolly.univeq._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import shipreq.webapp.base.data.Svg
import shipreq.webapp.base.jsfacade.{ReactSvgPanZoom, ReactVirtualized}

object SvgPanZoom {

  final case class Props(svg: Svg) {
    @inline def render: VdomElement = Component(this)
  }

  final case class State(svg  : Svg,
                         value: ReactSvgPanZoom.Value,
                         tool : ReactSvgPanZoom.Tool)

  private val container = <.div(^.width := "100%", ^.height := "100%")

  private val miniatureProps =
    ReactSvgPanZoom.MiniatureProps(position = ReactSvgPanZoom.Exports.POSITION_RIGHT)

  final class Backend($: BackendScope[Props, State]) {
    def render(p: Props, s: State): VdomNode = {
      p.svg.vdom
      container(
        ReactVirtualized.AutoSize { dims =>
          val props = ReactSvgPanZoom.Props(
            width          = dims.width,
            height         = dims.height,
            value          = s.value,
            onChangeValue  = v => $.modState(_.copy(value = v)),
            tool           = s.tool,
            onChangeTool   = t => $.modState(_.copy(tool = t)),
            detectAutoPan  = false,
            background     = "white",
            miniatureProps = miniatureProps,
          )
          ReactSvgPanZoom.Component(props)(
            p.svg.vdom
          )
        }
      )
    }
  }

  implicit val reusabilityProps: Reusability[Props] = Reusability.derive
  implicit val reusabilityState: Reusability[State] = Reusability.byRef

  private def deriveState(p: Props, prevState: Option[State]): State =
    prevState match {
      case Some(s) if s.svg ==* p.svg =>
        s // no state change
      case _ =>
        State(
          svg   = p.svg,
          value = ReactSvgPanZoom.Exports.INITIAL_VALUE,
          tool  = ReactSvgPanZoom.Exports.TOOL_AUTO,
        )
    }

  val Component = ScalaComponent.builder[Props]
    .getDerivedStateFromPropsAndState(deriveState)
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build
}
