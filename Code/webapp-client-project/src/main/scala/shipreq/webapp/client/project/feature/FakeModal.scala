package shipreq.webapp.client.project.feature

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement

trait FakeModal {
  def render: VdomElement
}

/** This is a cheap alternative to using a real modal dialog.
  *
  * It has two states: None and Some(m). When the state is None, there is no modal visible and therefore calling
  * `renderOrElse` returns the default screen. When the state is Some(m), there is a modal visble and the default view
  * is not rendered. This effectively allows you to toggle between the two.
  *
  * Unlike using a Semantic UI modal, this modal *replaces* the view that it would otherwise overlap.
  * However, it means that we can use React normally for everything, as opposed to using a Semantic UI modal which
  * requires us to manually manipulate the DOM and use DOM-based callbacks for state management.
  *
  * Usage:
  *   1. Add `modal: FakeModal.State` to your component state
  *   2. Render with `state.modal renderOrElse defaultView`
  *   3. To show the modal, set the state to `Some(FakeModal(modalContent))`
  *   4. To hide the modal, set the state to `None`
  */
object FakeModal {
  type State = Option[FakeModal]
  type SetFn = State ~=> Callback

  def none: State =
    None

  def apply(re: VdomElement): FakeModal =
    new FakeModal {
      override def render = re
    }

  @inline implicit def autoLiftOption(m: FakeModal): State =
    Some(m)

  @inline implicit class OptionFakeModalOps(private val o: State) extends AnyVal {
    @inline def renderOrElse(default: => VdomElement): VdomElement =
      o.fold(default)(_.render)
  }

  implicit val reuse: Reusability[State] =
    Reusability.option(Reusability.never[FakeModal])
}
