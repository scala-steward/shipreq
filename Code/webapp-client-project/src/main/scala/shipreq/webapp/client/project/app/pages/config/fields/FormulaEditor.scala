package shipreq.webapp.client.project.app.pages.config.fields

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.base.util._
import shipreq.webapp.base.ui.semantic.{Button, Icon, UsesSemanticUiManually}
import shipreq.webapp.client.project.app.Style.{formulaFieldEditor => *}
import shipreq.webapp.client.project.util.DataReusability._
import shipreq.webapp.client.project.widgets.FormulaHelp

object FormulaEditor {

  final case class Props(state   : String,
                         onChange: String => Callback,
                         error   : Option[ErrorMsg],
                         enabled : Enabled) {
    @inline def render: VdomElement = Component(this)
  }

  implicit val reusabilityProps: Reusability[Props] =
    Reusability.caseClassExcept("onChange") // used via $.props.flatMap in event handler which is reuse-safe

  private val helpButton: VdomTag =
    Button(tipe = Button.Type.IconOnly(Icon.HelpCircle))
      .tag(^.onClick --> FormulaHelp.modal.show)

  final class Backend($: BackendScope[Props, Unit]) {

    private val onChange: ReactEventFromInput => Callback = e =>
      $.props.flatMap(p => p.onChange(e.target.value))

    @UsesSemanticUiManually
    def render(p: Props): VdomNode =
      React.Fragment(

        <.div(^.cls := "ui input right action",
          (^.cls := "error").when(p.error.isDefined),

          <.input.text(
            ^.value := p.state,
            ^.onChange ==> onChange,
            ^.placeholder := "Formula…",
            ^.disabled := p.enabled.is(Disabled),
          ),

          helpButton,
        ),

        p.error.map(err =>
          <.div(*.applicableReqTypesErrMsg, err.value))
      )
  }

  val Component = ScalaComponent.builder[Props]
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build
}
