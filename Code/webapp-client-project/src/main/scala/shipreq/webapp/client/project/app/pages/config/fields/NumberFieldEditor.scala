package shipreq.webapp.client.project.app.pages.config.fields

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.ReactMonocle._
import japgolly.scalajs.react.vdom.html_<^._
import monocle.macros.Lenses
import scalacss.ScalaCssReact._
import shipreq.base.util._
import shipreq.webapp.base.ui.widgets.Form
import shipreq.webapp.base.validation.lib.CommonValidation
import shipreq.webapp.base.validation.lib.Simple.{Invalidity, Invalidator}
import shipreq.webapp.base.validation.ValidationUX
import shipreq.webapp.client.project.app.Style.{numberFieldEditor => *}
import shipreq.webapp.client.project.util.DataReusability._
import shipreq.webapp.client.project.widgets.ReqTypeRulesEditor
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.event.CustomNumberFieldGD
import shipreq.webapp.member.project.protocol.websocket.UpdateConfigCmd
import shipreq.webapp.member.ui.AutosizeTextarea

object NumberFieldEditor {
  import DataImplicits._

  def FieldNameDesc          = "Description"
  def FieldNameMin           = "Minimum Value (inclusive)"
  def FieldNameMax           = "Maximum Value (inclusive)"
  def FieldNameDecimalPlaces = "Decimal Places"

  final case class Props(state     : StateSnapshot[State],
                         cfg       : ProjectConfig,
                         filterDead: FilterDead,
                         enabled   : Enabled) {

    val validatorState: DataValidators.field.State =
      state.value.validatorState(cfg)

    @inline def render: VdomElement = Component(this)
  }

  @Lenses
  final case class State(idOption     : Option[CustomField.Number.Id],
                         name         : String,
                         desc         : String,
                         min          : String,
                         max          : String,
                         decimalPlaces: String,
                         rules        : ReqTypeRulesEditor.DoubleDefault.State) {

    def validatorState(cfg: ProjectConfig): DataValidators.field.State =
      DataValidators.field.State.from(idOption, cfg)

    def updateCmd(cfg: ProjectConfig): PotentialChange[Unit, UpdateConfigCmd.ToModifyFields] = {
      val vs = validatorState(cfg)

      val pass1 =
        for {
          _name  <- PotentialChange.fromDisjunction(DataValidators.field.name(vs).unnamed(name).leftMap(_ => ()))
          _desc  <- PotentialChange.fromDisjunction(DataValidators.numberField.desc.unnamed(desc).leftMap(_ => ()))
          range  <- PotentialChange.fromDisjunction(DataValidators.numberField.range.unnamed((min, max)).leftMap(_ => ()))
          dp     <- PotentialChange.fromDisjunction(DataValidators.numberField.decimalPlaces.unnamed(decimalPlaces).leftMap(_ => ()))
          _rules <- PotentialChange.needFromOption(rules.validation(cfg.reqTypes).resultWhenValid(validateByRange(range)))
        } yield (_name, _desc, range, dp, _rules)

      pass1.flatMap { case (name, desc, range, decimalPlaces, rules) =>
        idOption match {

          case Some(id) =>
            val old = cfg.fields.custom(id)
            val b = CustomNumberFieldGD.valueBuilder()
            b.addIfChanged(CustomNumberFieldGD.Name             )(old.name             , name)
            b.addIfChanged(CustomNumberFieldGD.Desc             )(old.desc             , desc)
            b.addIfChanged(CustomNumberFieldGD.Range            )(old.range            , range)
            b.addIfChanged(CustomNumberFieldGD.DecimalPlaces    )(old.decimalPlaces    , decimalPlaces)
            b.addIfChanged(CustomNumberFieldGD.FieldReqTypeRules)(old.fieldReqTypeRules, rules)
            PotentialChange.fromOption(b.nev()).map { newValues =>
              UpdateConfigCmd.CustomFieldUpdateNumber(id, newValues)
            }

          case None =>
            val cmd = UpdateConfigCmd.CustomFieldCreateNumber(
              name              = name,
              desc              = desc,
              min               = range._1,
              max               = range._2,
              decimalPlaces     = decimalPlaces,
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
        idOption      = None,
        name          = "",
        desc          = "",
        min           = "0",
        max           = "10",
        decimalPlaces = "0",
        rules         = ReqTypeRulesEditor.State.empty,
      )

    def init(id: CustomField.Number.Id, cfg: ProjectConfig): State = {
      val f = cfg.fields.custom(id)
      apply(
        idOption      = Some(id),
        name          = f.name,
        desc          = f.desc.getOrElse(""),
        min           = f.min.toString,
        max           = f.max.toString,
        decimalPlaces = f.decimalPlaces.toString,
        rules         = ReqTypeRulesEditor.State.init(cfg, f.fieldReqTypeRulesByResolution)(_.toString),
      )
    }

    def init(id: Option[CustomField.Number.Id], cfg: ProjectConfig): State =
      id.fold(empty)(init(_, cfg))
  }

  // ===================================================================================================================

  private def validateByRange(range: (Double, Double)): Double => Validity =
    validateByRange(range._1, range._2)

  private def validateByRange(min: Double, max: Double): Double => Validity =
    if (min <= max)
      d => Valid.when(d >= min && d <= max)
    else
      _ => Valid

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
        .withValidator(DataValidators.numberField.desc.unnamed)
        .withEnabled(p.enabled)

    val minValueOption = p.state.value.min.toDoubleOption
    val maxValueOption = p.state.value.max.toDoubleOption

    val minValidator =
      CommonValidation.double
        .appendInvalidator(Invalidator(min =>
          maxValueOption.flatMap(max =>
            Option.when(min > max)(Invalidity("Can't be greater than the maximum.")))))

    val maxValidator =
      CommonValidation.double
        .appendInvalidator(Invalidator(max =>
          minValueOption.flatMap(min =>
            Option.when(min > max)(Invalidity("Can't be less than the minimum.")))))

    val minField =
      Form.Field.text
        .withLabel(FieldNameMin)
        .withState(p.state.zoomStateL(State.min))
        .withValidator(minValidator)
        .withEnabled(p.enabled)

    val maxField =
      Form.Field.text
        .withLabel(FieldNameMax)
        .withState(p.state.zoomStateL(State.max))
        .withValidator(maxValidator)
        .withEnabled(p.enabled)

    val decimalPlacesField =
      Form.Field.text
        .withLabel(FieldNameDecimalPlaces)
        .withState(p.state.zoomStateL(State.decimalPlaces))
        .withValidator(DataValidators.numberField.decimalPlaces.unnamed)
        .withEnabled(p.enabled)

    val reqTypeRulesEditorDefaultWidget: ReqTypeRulesEditor.DefaultWidgetFn[Double] =
      Reusable.implicitly((minValueOption, maxValueOption)).map { _ =>

        var validator = CommonValidation.double
        (minValueOption, maxValueOption) match {
          case (Some(min), Some(max)) =>
            val isValid = validateByRange(min, max)
            validator = validator.appendInvalidator(Invalidator(d =>
              Option.when(isValid(d) is Invalid)(Invalidity("Out of range."))
            ))
          case _ =>
        }

        (ss, enabled, _) => {
          def onChange(e: ReactEventFromInput): Callback = {
            val newTxt   = validator.corrector.live(e.target.value)
            val newDbl   = validator(newTxt).toOption
            val newState = ss.value.copy(textValue = newTxt, default = newDbl)
            ss.setState(newState)
          }

          val validated = validator(ss.value.textValue)

          <.div(^.cls := "ui input",
            *.reqTypeRuleDefaultEditor,
            (^.cls := "error").when(validated.isLeft),

            <.input.text(
              ^.value := ss.value.textValue,
              ^.onChange ==> onChange,
              ^.disabled := enabled.is(Disabled),
            ),

            validated.swap.toOption.map(err => <.div(*.applicableReqTypesErrMsg, Invalidity.toText(err)))
          )
        }
      }

    val rules =
      ReqTypeRulesEditor.DoubleDefault.Component(
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
        Form.Field.two(
          minField,
          maxField,
        ),
        decimalPlacesField,
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
