package shipreq.webapp.base.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.webapp.base.jsfacade.Screenfull
import shipreq.webapp.base.ui.{BaseStyles => *}

trait OptionalFullscreen {
  def apply(f: OptionalFullscreen.Ctx => VdomNode): VdomNode
}

object OptionalFullscreen {

  final case class Ctx(currentlyFullscreen: Boolean,
                       toggleFullscreen   : Callback) {

    def enterFullscreen: Option[Callback] =
      Option.unless(currentlyFullscreen)(toggleFullscreen)

    def exitFullscreen: Option[Callback] =
      Option.when(currentlyFullscreen)(toggleFullscreen)
  }

  implicit def reusability: Reusability[OptionalFullscreen] =
    Reusability.byRef

  val real: OptionalFullscreen = {
    val browserFullscreenEnter: Callback =
      Callback {
        if (Screenfull.isEnabled)
          Screenfull.request()
      }

    val browserFullscreenExit: Callback =
      Callback {
        if (Screenfull.isEnabled)
          Screenfull.exit()
      }

    val impl = new Impl(browserFullscreenEnter, browserFullscreenExit)

    impl.Component(_)
  }

  final class Impl(fullscreenEnter: Callback, fullscreenExit: Callback) {

    type Props = Ctx => VdomNode

    case class State(fullscreen: Boolean) {
      def toggle = State(!fullscreen)
    }

    object State {
      def init: State =
        apply(fullscreen = false)
    }

    private val fullscreenContainer =
      <.div(*.fullscreen)

    final class Backend($: BackendScope[Props, State]) {

      private val toggleFullscreen: Callback =
        for {
          s1 <- $.state
          s2 <- CallbackTo.pure(s1.toggle)
          _  <- $.setState(s2)
          _  <- if (s2.fullscreen) fullscreenEnter else fullscreenExit
        } yield ()

      def render(p: Props, s: State): VdomNode = {
        val ctx = Ctx(s.fullscreen, toggleFullscreen)
        val inner = p(ctx)
        if (s.fullscreen)
          fullscreenContainer(inner)
        else
          inner
      }

      val onUnmount =
        $.state.flatMap(s => fullscreenExit.when_(s.fullscreen))
    }

    val Component = ScalaComponent.builder[Props]
      .initialState(State.init)
      .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount)
      .build
  }
}