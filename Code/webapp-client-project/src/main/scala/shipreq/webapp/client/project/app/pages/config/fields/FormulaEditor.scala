package shipreq.webapp.client.project.app.pages.config.fields

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import scalacss.ScalaCssReact._
import shipreq.base.util._
import shipreq.webapp.base.ui.semantic.{Button, Icon, UsesSemanticUiManually}
import shipreq.webapp.client.project.app.Style.{formulaFieldEditor => *}
import shipreq.webapp.client.project.util.DataReusability._
import shipreq.webapp.client.project.widgets.FormulaHelp
import shipreq.webapp.member.feature.AutoCompleteFeature._
import shipreq.webapp.member.project.data.ProjectConfig
import shipreq.webapp.member.project.formula.{FormulaAlgebra, FormulaFunction}

object FormulaEditor {

  final case class Props(state        : String,
                         onChange     : String => Callback,
                         error        : Option[ErrorMsg],
                         enabled      : Enabled,
                         projectConfig: ProjectConfig) {
    @inline def render: VdomElement = Component(this)
  }

  implicit val reusabilityProps: Reusability[Props] =
    Reusability.caseClassExcept("onChange") // used via $.props.flatMap in event handler which is reuse-safe

  def autoCompleteStrategies(cfg: ProjectConfig): AutoComplete.Strategies = {
    val fieldSuggestions =
      (cfg.liveCustomFormulaFields.map(_.name) ++ cfg.liveCustomNumberFields.map(_.name))
        .map(name => "field:" + FormulaAlgebra.quoteFieldName(name))

    val functionSuggestions =
      FormulaFunction.all.map(_.name + "(")

    val suggestions = (fieldSuggestions ++ functionSuggestions.whole).sorted

    val main =
      AutoComplete.Strategy.builder
        .regex("""\b([a-zA-Z0-9":]+)$""", index = 1)
        .search { s0 =>
          val s = s0.toLowerCase
          val r = suggestions.filter(t => t.length > s.length && t.toLowerCase.startsWith(s)).take(MaxResults)
          // println(s"[$s0] -> $r")
          r
        }
        .replace2 { t =>
          val e = if (t.endsWith("(")) ")" else ""
          (t, e)
        }
        .result()

    Vector(main)
  }

  private def helpButton(enabled: Enabled): VdomTag =
    Button(tipe = Button.Type.IconOnly(Icon.HelpCircle))
      .tag(
        ^.disabled := (enabled is Disabled),
        ^.onClick  --> FormulaHelp.modal.show,
      )

  final class Backend($: BackendScope[Props, Unit]) extends AutoComplete.BackendI {

    override protected def getTextFromHeadToCaret =
      AutoComplete.getTextFromHeadToCaretI

    private val pxProjectConfig: Px[ProjectConfig] =
      Px.props($).map(_.projectConfig).withReuse.autoRefresh

    private val pxAutoComplete: Px[AutoComplete.Strategies] =
      pxProjectConfig.map(autoCompleteStrategies)

    private val inputDomRef = Ref[html.Input]

    override val autoCompleteCtx: CallbackOption[AutoCompleteCtx] =
      inputDomRef.get.asCBO.map(AutoCompleteCtx(pxAutoComplete.value(), _))

    private val onChange: ReactEventFromInput => Callback = e =>
      $.props.flatMap(p => p.onChange(e.target.value))

    @UsesSemanticUiManually
    def render(p: Props): VdomNode =
      React.Fragment(

        <.div(^.cls := "ui input right action",
          (^.cls := "error").when(p.error.isDefined),

          <.input.text(
            ^.disabled          := p.enabled.is(Disabled),
            ^.onBlur           --> autoCompleteOnBlur,
            ^.onChange         ==> onChange,
            ^.onClick          ==> autoCompleteOnClick,
            ^.onKeyDown        ==> autoCompleteOnKeyDown,
            ^.onKeyDownCapture ==> autoCompleteOnKeyDownCapture,
            ^.placeholder       := "Formula…",
            ^.value             := p.state,
          ).withRef(inputDomRef),

          helpButton(p.enabled),
        ),

        p.error.map(err =>
          <.div(*.applicableReqTypesErrMsg, err.value))
      )
  }

  val Component = ScalaComponent.builder[Props]
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .configure(AutoComplete.install(autoCompletableInput))
    .build
}
