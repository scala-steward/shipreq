package shipreq.webapp.client.base.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import shipreq.webapp.base.UiText
import shipreq.webapp.base.validation.ValidatorU
import shipreq.webapp.client.base.feature.AsyncActionFeature
import shipreq.webapp.client.base.ui.semantic._

object TextInputAndButton {

  sealed abstract class State
  object State {
    case object Blank                   extends State
    case object InTransit               extends State
    case class Ready(commit: Callback)  extends State
    case class InputError(err: TagMod)  extends State
    case class AsyncError(err: TagMod, retry: Callback)  extends State

    def async[A](a: AsyncActionFeature.D0.State[A])(implicit f: A => TagMod): Option[State] =
      a.map {
        case AsyncActionFeature.Locked          => InTransit
        case AsyncActionFeature.Failed(e, r, _) => AsyncError(f(e), r)
      }

    def validator[I, C, V](v: ValidatorU[I, C, V])(i: I, filter: C => Boolean, commit: V => Callback): State = {
      val corrected = v.correctedU(i)
      if (filter(corrected.value))
        v.validateU(corrected) match {
          case scalaz.Success(ok)  => State.Ready(commit(ok))
          case scalaz.Failure(err) => State.InputError(err.toText)
        }
      else
        Blank
    }
  }

  final case class Props(text       : String,
                         updateText : String => Callback,
                         state      : State,
                         placeholder: String,
                         buttonLabel: String) {
    @inline def render = Component(this)
  }

  object Props {
    def asyncAware[A](asyncState   : AsyncActionFeature.D0.State[A],
                      asyncFeature : AsyncActionFeature.D0.Feature[A],
                      text         : String,
                      updateText   : String => Callback,
                      nonAsyncState: => State,
                      placeholder  : String,
                      buttonLabel  : String
                     )(implicit f: A => TagMod): Props = {

      val (updText, state) =
        State.async(asyncState) match {
          case Some(s) => (updateText.andThen(_ >> asyncFeature.clearError(asyncState)), s)
          case None    => (updateText, nonAsyncState)
        }

      Props(text, updText, state, placeholder, buttonLabel)
    }
  }

//  implicit val reusabilityProps: Reusability[Props] =
//    Reusability.caseClass

  private val errLabel = Label.Style(Label.Type.BasicPointingUp, Colour.Red).div

  val buttonOk       = Button(`type` = Button.Type.Primary)
  val buttonDisabled = Button(`type` = Button.Type.Primary,  state = Button.State.Disabled)
  val buttonError    = Button(`type` = Button.Type.Negative, state = Button.State.Disabled)
  val buttonLoading  = Button(`type` = Button.Type.Primary,  state = Button.State.Loading)

  final class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement = {
      val onChange = (_: ReactEventI).extract(_.target.value)(p.updateText)

      val input =
        <.input.text(
          ^.placeholder := p.placeholder,
          ^.value       := p.text,
          ^.onChange   ==> onChange)

      p.state match {

        case State.Blank =>
          <.div(
            <.div(
              Input.Action(
                input,
                buttonDisabled.tag(p.buttonLabel))))

        case State.Ready(commit) =>
          <.div(
            <.div(
              Input.Action(
                input, // should redirect Enter to commit
                buttonOk.tag(^.onClick --> commit, p.buttonLabel))))

        case State.InTransit =>
          <.div(
            <.div(
              Input.Action(
                input(^.disabled := "disabled"),
                buttonLoading.tag(p.buttonLabel))))

        case State.InputError(err) =>
          <.div(
            <.div(
              Input.ActionError(
                input,
                buttonError.tag(p.buttonLabel))),
            errLabel(err))

        case State.AsyncError(err, retry) =>
          <.div(
            <.div(
              Input.ActionError(
                input,
                buttonOk.tag(^.onClick --> retry, UiText.buttonRetry))),
            errLabel(err))
      }
    }
  }

  val Component = ReactComponentB[Props]("TI&B")
    .renderBackend[Backend]
//    .configure(Reusability.shouldComponentUpdate)
    .build
}
