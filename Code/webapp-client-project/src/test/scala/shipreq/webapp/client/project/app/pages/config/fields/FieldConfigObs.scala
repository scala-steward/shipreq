package shipreq.webapp.client.project.app.pages.config.fields

import org.scalajs.dom.html
import shipreq.base.util.{Disabled, Enabled}
import shipreq.webapp.base.test.TestState._
import shipreq.webapp.base.ui.semantic.Input
import shipreq.webapp.client.project.app.Style
import shipreq.webapp.client.project.app.pages.config.Buttons
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.test.CommonObs

object FieldConfigObs {

  lazy val selNewButton         = Style.widgets.dropdownButtonOuter       .selector
  lazy val selRightOn           = Style.widgets.splitScreenCrud.rightOn   .selector
  lazy val selEmptyRight        = Style.widgets.splitScreenCrud.emptyRight.selector
  lazy val selEditorButtons     = Style.tagConfig.editorButtons           .selector
  lazy val selRulesDeadReqTypes = Style.fieldConfig.rulesDeadReqTypesInner.selector

  final class FieldList($: DomZipperJs) {
    val rows: Vector[FieldListRow] =
      $("tbody").children1n("tr").map(new FieldListRow(_))

    def apply(name: String): FieldListRow =
      rows.find(_.name ==* name).getOrElse(throw new RuntimeException("Field not found: " + name))
  }

  final class FieldListRow($: DomZipperJs) {
    val rowDom = $.domAsHtml
    val name   = $.child("td", 2 of 5).domAsHtml.textContent
    val detail = $.child("td", 4 of 5).domAsHtml.textContent
  }

  final class Editor($: DomZipperJs) {
    private val firstField = $.collect0n(".ui.form .field").zippers.headOption

    val message = $.collect01(".ui.message").map(new CommonObs.Message(_))

    val nameDom   = firstField.flatMap(_.collect01("input").domsAs[html.Input])
    val nameValue = nameDom.map(_.value)
    val nameError = firstField.filter(_ => nameDom.isDefined).flatMap(_.children01("span").zippers.map(_.domAsHtml.textContent.trim))

    val dropdown = firstField.filter(_.exists(".menu")).map(new CommonObs.Dropdown(_))

    val rules = $.collect01("table.ui.single.line").map(new Rules(_))

    lazy val editables =
      collectSemanticUi($, Enabled).doms

    private def fieldByLabel(label: String): Option[DomZipperJs] =
      $.collect0n(".field")
        .filter(_.collect0n("label").zippers.exists(_.domAsHtml.textContent.trim ==* label))
        .zippers
        .headOption

    def fieldInputDom(label: String): Option[html.Input] =
      fieldByLabel(label).flatMap(_.collect01("input").domsAs[html.Input])

    def fieldError(label: String): Option[String] =
      fieldByLabel(label).flatMap(_.collect01("span").zippers.map(_.domAsHtml.textContent.trim))

    val descDom: Option[html.Input] = fieldInputDom(NumberFieldEditor.FieldNameDesc)
    val descValue: Option[String] = descDom.map(_.value)
    val descError: Option[String] = fieldError(NumberFieldEditor.FieldNameDesc)

    val minDom: Option[html.Input] = fieldInputDom(NumberFieldEditor.FieldNameMin)
    val minValue: Option[String] = minDom.map(_.value)
    val minError: Option[String] = fieldError(NumberFieldEditor.FieldNameMin)

    val maxDom: Option[html.Input] = fieldInputDom(NumberFieldEditor.FieldNameMax)
    val maxValue: Option[String] = maxDom.map(_.value)
    val maxError: Option[String] = fieldError(NumberFieldEditor.FieldNameMax)

    val decimalPlacesDom: Option[html.Input] = fieldInputDom(NumberFieldEditor.FieldNameDecimalPlaces)
    val decimalPlacesValue: Option[String] = decimalPlacesDom.map(_.value)
    val decimalPlacesError: Option[String] = fieldError(NumberFieldEditor.FieldNameDecimalPlaces)
  }

  final class Rules($: DomZipperJs) {
    val rows = $.child("tbody").children1n.map(new RuleRowObs(_))
  }

  final class RuleRowObs($: DomZipperJs) {
    private val reqTypes = $.child("td", 1 of 3)
    private val rule     = $.child("td", 2 of 3)
    private val button   = $.child("td", 3 of 3)

    val reqTypesDom       = reqTypes.collect01("input").domsAs[html.Input]
    val reqTypesError     = reqTypes.collect01(s"[${Input.errorAttr.attrName}]").zippers.map(_.domAsHtml.textContent.trim)
    val deadReqTypes      = reqTypes.collect01(selRulesDeadReqTypes).zippers.map(_.domAsHtml.textContent.trim)
    val res               = new CommonObs.Dropdown(rule(".ui.dropdown.selection:first-child"))
    val default           = rule.collect01(".ui.dropdown.selection:not(:first-child)").map(new CommonObs.Dropdown(_))
    val defaultInputDom   = rule.collect01(".ui.input input").domsAs[html.Input]
    val defaultInputValue = defaultInputDom.map(_.value)
    val defaultInputError = rule.collect01(".ui.input div").zippers.map(_.domAsHtml.textContent.trim)
    val dead              = button.collect01("button").isEmpty

    val reqTypesDesc: String =
      reqTypesDom match {
        case Some(r) if !dead => r.value
        case _                => reqTypes.domAsHtml.textContent.replace("Dead req types:", "").trim
      }

    val desc = RuleRow(
      reqTypes      = reqTypesDesc,
      rule          = res.selected.getOrElse(""),
      default       = default.map(_.selected.getOrElse("")).orElse(defaultInputValue),
      defaultError  = default.fold(false)(_.hasError) || defaultInputError.isDefined,
      dead          = dead,
      reqTypesError = reqTypesError,
    )

    val addButton = button.collect01("button.green").domsAsHtml
    val delButton = button.collect01("button.negative").domsAsHtml
  }
}

// =====================================================================================================================

final class FieldConfigObs($: DomZipperJs) {
  import FieldConfigObs._

  val left  = $.child("section", 1 of 2)
  val right = $.child("section", 2 of 2).child(selRightOn)

  val filterDeadButton = left.child("div").child("div", 2 of 2)("button").dom
  val filterDead       = ShowDead.when(filterDeadButton.classList.contains("red"))

  val fieldList = new FieldList(left)

  val isEditorOpen: Boolean =
    !right.exists(selEmptyRight)

  val buttonDoms: Buttons[html.Button] =
    if (isEditorOpen)
      Buttons.obs(right(selEditorButtons))
    else
      Buttons.none

  val buttonsEnabled: Buttons[Enabled] =
    buttonDoms.map(Disabled when _.disabled)

  val editor: Option[Editor] =
    Option.when(isEditorOpen)(new Editor(right.child("div").child("div", 1 of 2)))

  val newButton =
    new CommonObs.DropdownButton(left(selNewButton))
}
