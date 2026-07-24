package shipreq.webapp.client.project.widgets

import japgolly.scalajs.react.vdom.html_<^._
import shipreq.webapp.client.project.widgets.HelpModal._
import shipreq.webapp.member.project.formula._

object FormulaHelp {

  private final val score = "field:Score"
  private final val urgency = "field:Urgency"

  val modal = HelpModal("Formula Help", Groups(

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    Group("Basics")(

      Row(
        "Basic arithmetic operators are supported, including addition, subtraction, multiplication, division and parentheses for grouping."
      )("(1 + 2) * 3 / 4"),

      Row.nev(
        "Values can be compared using typical comparison operators."
      )(FormulaCmpOp.all.map(op => s"A ${op.symbol} B": VdomNode)),
    ),

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    Group("Fields")(

      Row(
        "To reference the value of a number field, use ",
        code("field:<NAME>"), " and replace ", code("<NAME>"), " with the real field name."
      )("field:Priority"),

    ),

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    Group.nev("Functions")(

      FormulaFunction.all.sortBy(_.name).map {

        case f@ FormulaFunction.And =>
          Row(s"${f.name}(values): returns true if all values are true")(
            s"${f.name}(ISBLANK($score), ISBLANK($urgency))")

        case f@ FormulaFunction.Average =>
          Row(s"${f.name}(values): returns the average of the values")(
            s"${f.name}($score, $urgency)")

        case f@ FormulaFunction.Ceiling =>
          Row(s"${f.name}(value): returns the smallest integer greater than or equal to the value")(
            s"${f.name}($score)")

        case f@ FormulaFunction.Err =>
          Row(s"${f.name}(reason): raises an error")(
            s"${f.name}(\"Invalid priority\")")

        case f@ FormulaFunction.Floor =>
          Row(s"${f.name}(value): returns the largest integer less than or equal to the value")(
            s"${f.name}($score)")

        case f@ FormulaFunction.If =>
          Row(
            s"${f.name}(logical_test, value_if_true, [value_if_false]): ",
            "returns one value if a condition is true and another value (or blank) if it's false."
          )(s"${f.name}($score >= 50, \"Pass\", \"Fail\")")

        case f@ FormulaFunction.IsBlank =>
          Row(s"${f.name}(value): returns true if the value is blank")(
            s"${f.name}($score)")

        case f@ FormulaFunction.IsBool =>
          Row(s"${f.name}(value): returns true if the value is a boolean")(
            s"${f.name}(1 = 1)")

        case f@ FormulaFunction.IsErr =>
          Row(s"${f.name}(value): returns true if the value is an error")(
            s"${f.name}($score / 0)")

        case f@ FormulaFunction.IsNumber =>
          Row(s"${f.name}(value): returns true if the value is a number")(
            s"${f.name}($score)")

        case f@ FormulaFunction.IsText =>
          Row(s"${f.name}(value): returns true if the value is text")(
            s"${f.name}(\"Hello\")")

        case f@ FormulaFunction.Max =>
          Row(s"${f.name}(values): returns the maximum value from a list of values")(
            s"${f.name}($score, $urgency)")

        case f@ FormulaFunction.Min =>
          Row(s"${f.name}(values): returns the minimum value from a list of values")(
            s"${f.name}($score, $urgency)")

        case f@ FormulaFunction.Not =>
          Row(s"${f.name}(value): returns the negation of a boolean value")(
            s"${f.name}(ISBLANK($score))")

        case f@ FormulaFunction.Or =>
          Row(s"${f.name}(values): returns true if any of the values are true")(
            s"${f.name}(ISBLANK($score), ISBLANK($urgency))")

        case f@ FormulaFunction.Round =>
          Row(s"${f.name}(number, [num_digits]): rounds a given number to a specified number of decimal places (or 0)")(
            s"${f.name}($score)",
            s"${f.name}($score, 2)")
      }

    ),

  ))
}
