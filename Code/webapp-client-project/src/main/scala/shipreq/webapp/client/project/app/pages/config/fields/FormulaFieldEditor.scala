package shipreq.webapp.client.project.app.pages.config.fields

import japgolly.scalajs.react.ReactMonocle._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import monocle.macros.Lenses
import scalacss.ScalaCssReact._
import shipreq.base.util._
import shipreq.webapp.base.ui.semantic.UsesSemanticUiManually
import shipreq.webapp.base.ui.widgets.Form
import shipreq.webapp.base.validation.ValidationUX
import shipreq.webapp.client.project.app.Style.{formulaFieldEditor => *}
import shipreq.webapp.client.project.util.DataReusability._
import shipreq.webapp.client.project.widgets.ReqTypeRulesEditor
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.formula.{Formula, FormulaParser, ValidFormula}
import shipreq.webapp.member.project.event.CustomFormulaFieldGD
import shipreq.webapp.member.project.protocol.websocket.UpdateConfigCmd
import shipreq.webapp.member.ui.AutosizeTextarea

object FormulaFieldEditor {
  import DataImplicits._

  def FieldNameDesc = "Description"

  final case class Props(state     : StateSnapshot[State],
                         cfg       : ProjectConfig,
                         filterDead: FilterDead,
                         enabled   : Enabled) {

    val validatorState: DataValidators.field.State =
      state.value.validatorState(cfg)

    @inline def render: VdomElement = Component(this)
  }

  @Lenses
  final case class State(idOption: Option[CustomField.Formula.Id],
                         name    : String,
                         desc    : String,
                         rules   : ReqTypeRulesEditor.ForFormulaFields.State) {

    def validatorState(cfg: ProjectConfig): DataValidators.field.State =
      DataValidators.field.State.from(idOption, cfg)

    def updateCmd(cfg: ProjectConfig): PotentialChange[Unit, UpdateConfigCmd.ToModifyFields] = {
      val vs = validatorState(cfg)

      val pass1 =
        for {
          _name  <- PotentialChange.fromDisjunction(DataValidators.field.name(vs).unnamed(name).leftMap(_ => ()))
          _desc  <- PotentialChange.fromDisjunction(DataValidators.formulaField.desc.unnamed(desc).leftMap(_ => ()))
          _rules <- PotentialChange.needFromOption(rules.validation(cfg.reqTypes).resultWhenValid(_ => Valid)) // TODO
        } yield (_name, _desc, _rules)

      pass1.flatMap { case (name, desc, rules) =>
        idOption match {

          case Some(id) =>
            val old = cfg.fields.custom(id)
            val b = CustomFormulaFieldGD.valueBuilder()
            b.addIfChanged(CustomFormulaFieldGD.Name             )(old.name             , name)
            b.addIfChanged(CustomFormulaFieldGD.Desc             )(old.desc             , desc)
            b.addIfChanged(CustomFormulaFieldGD.FieldReqTypeRules)(old.fieldReqTypeRules, rules)
            PotentialChange.fromOption(b.nev()).map { newValues =>
              UpdateConfigCmd.CustomFieldUpdateFormula(id, newValues)
            }

          case None =>
            val cmd = UpdateConfigCmd.CustomFieldCreateFormula(
              name              = name,
              desc              = desc,
              fieldReqTypeRules = rules,
            )
            PotentialChange.Success(cmd)
        }
      }
    }
  }

  object State {
    def empty: State =
      apply(
        idOption = None,
        name     = "",
        desc     = "",
        rules    = ReqTypeRulesEditor.State.empty,
      )

    def init(id: CustomField.Formula.Id, cfg: ProjectConfig): State = {
      val f = cfg.fields.custom(id)
      apply(
        idOption = Some(id),
        name     = f.name,
        desc     = f.desc.getOrElse(""),
        rules    = ReqTypeRulesEditor.State.init(cfg, f.fieldReqTypeRulesByResolution)(
                     v => Formula.Valid.toText(v.formula, cfg.fields)),
      )
    }

    def init(id: Option[CustomField.Formula.Id], cfg: ProjectConfig): State =
      id.fold(empty)(init(_, cfg))
  }

  // ===================================================================================================================

  private def render(p: Props): VdomNode = {

    val nameField =
      Form.Field.text
        .withLabel("Name")
        .withState(p.state.zoomStateL(State.name))
        .withValidator(DataValidators.field.name.unnamedFn(p.validatorState))
        .withEnabledAndAutoFocus(p.enabled)

    val descField =
      Form.Field.text
        .withEditor(AutosizeTextarea.editor)
        .withLabel(FieldNameDesc)
        .withState(p.state.zoomStateL(State.desc))
        .withValidator(DataValidators.formulaField.desc.unnamed)
        .withEnabled(p.enabled)

    def parseAndValidateFormula(txt: String): ErrorMsg \/ ValidFormula =
      FormulaParser.parse(txt).leftMap(_ => ErrorMsg("Invalid formula."))
        .flatMap(Formula.Potential.validate(_, p.cfg.fields))
        .map(ValidFormula(_))

    @UsesSemanticUiManually
    val reqTypeRulesEditorDefaultWidget: ReqTypeRulesEditor.DefaultWidgetFn[ValidFormula] =
      Reusable.byRef {
        (ss, enabled, _) => {
          def onChange(e: ReactEventFromInput): Callback = {
            val newTxt   = e.target.value
            val newFml   = parseAndValidateFormula(newTxt).toOption
            val newState = ss.value.copy(textValue = newTxt, default = newFml)
            ss.setState(newState)
          }

          val validated = parseAndValidateFormula(ss.value.textValue)

          <.div(^.cls := "ui input",
            *.reqTypeRuleDefaultEditor,
            (^.cls := "error").when(validated.isLeft),

            <.input.text(
              ^.value := ss.value.textValue,
              ^.onChange ==> onChange,
              ^.disabled := enabled.is(Disabled),
            ),

            validated.swap.toOption.map(err =>
              <.div(*.applicableReqTypesErrMsg, err.value))
          )
        }
      }

    val rules =
      ReqTypeRulesEditor.ForFormulaFields.Component(
        ReqTypeRulesEditor.Props(
          state         = p.state.zoomStateL(State.rules),
          reqTypes      = p.cfg.reqTypes,
          defaultWidget = reqTypeRulesEditorDefaultWidget,
          filterDead    = p.filterDead,
          enabled       = p.enabled))

    <.div(
      Form(
        nameField,
        descField,
      )(ValidationUX.Full),
      rules)
  }

  implicit val reusabilityState: Reusability[State] = Reusability.derive
  implicit val reusabilityProps: Reusability[Props] = Reusability.derive

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .configure(Reusability.shouldComponentUpdate)
    .build
}
