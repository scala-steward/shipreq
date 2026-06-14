package shipreq.webapp.client.project.widgets.editors_with_controls

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import shipreq.base.util._
import shipreq.webapp.base.feature.EditorStatus
import shipreq.webapp.base.validation.lib.CommonValidation
import shipreq.webapp.base.validation.lib.Simple.Invalidity
import shipreq.webapp.member.feature.EditControlsFeature
import shipreq.webapp.member.project.text.SingleLine
import shipreq.webapp.client.project.feature.editor.PotentialValueAcceptor

object NumberEditor {

  type Value = Option[Double]

  final case class Props(initialValue: Option[Value],
                         edit        : StateSnapshot[String],
                         asyncStatus : Option[EditorStatus.Async],
                         abort       : Option[Callback],
                         abortVerb   : String,
                         commitFn    : Option[Value => Callback],
                         commitVerb  : String,
                         autoFocus   : Boolean) {

    val parseResult: Invalidity \/ Value =
      CommonValidation.optionalDouble(edit.value)

    val validated: PotentialChange[Invalidity, Value] =
      PotentialChange.fromDisjunction(parseResult)

    val change: PotentialChange[Invalidity, Value] =
      validated.ignoreOption(initialValue)

    val commit: Option[Callback] =
      change.toOption.flatMap(v => commitFn.map(_ apply v))

    val status: EditorStatus =
      asyncStatus getOrElse EditorStatus.fromValidatedChange(change)(_ => commit, abort)

    @inline def render: VdomElement = Component(this)
  }

  implicit val reusabilityProps: Reusability[Props] =
    Reusability.byRef // because Props are memo'ised in NewEditor

  val potentialValueAcceptor: PotentialValueAcceptor[String] =
    PotentialValueAcceptor.correct(CommonValidation.optionalDouble.corrector.live)

  private val editControls =
    EditControlsFeature.Controls[Props](SingleLine)
      .abortWhenDefined(_.abort, _.abortVerb)
      .commitWhenDefined(_.status.getCommit, _.commitVerb)

  private def render(p: Props): VdomNode = {

    def editor(validity: Validity): VdomElement =
      <.input(
        ^.`type`    := "text",
        ^.value     := p.edit.value,
        ^.autoFocus := p.autoFocus,
        ^.onChange ==> ((e: ReactEventFromInput) => {
          val newVal = CommonValidation.optionalDouble.corrector.live(e.target.value)
          p.status.wrapEdit(p.edit.setState(newVal))
        }),
        (^.cls := "error").when(validity.is(Invalid)))

    EditControlsFeature.renderEditor(
      status       = p.status,
      editor       = editor,
      readOnlyView = p.edit.value,
      instructions = editControls.instructions(p))
  }

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .configure(Reusability.shouldComponentUpdate)
    .build
}
