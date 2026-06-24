package shipreq.webapp.member.project.formula

import japgolly.microlibs.testutil.TestUtil._
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.data.DataImplicits._
import shipreq.webapp.member.test.project.RandomData
import shipreq.webapp.member.test.project.UnsafeTypes._
import sourcecode.Line
import utest._

object FormulaPropTest extends TestSuite {

  private val scoreFieldId = 1.CFNum
  private val scoreField = CustomField.Number(
    id = scoreFieldId,
    name = "score",
    desc = None,
    range = (0.0, 100.0),
    decimalPlaces = 2,
    fieldReqTypeRules = FieldReqTypeRules.optional,
    liveExplicitly = Live
  )

  private val bonusFieldId = 2.CFNum
  private val bonusField = CustomField.Number(
    id = bonusFieldId,
    name = "bonus",
    desc = None,
    range = (0.0, 100.0),
    decimalPlaces = 1,
    fieldReqTypeRules = FieldReqTypeRules.optional.defaultTo(10.0)(CustomReqTypeId(1)),
    liveExplicitly = Live
  )

  private val fieldSet = FieldSet(
    emptyDataMap(CustomField).add(scoreField).add(bonusField),
    Vector(scoreFieldId, bonusFieldId)
  )

  private def parseOrDie(input: String)(implicit q: Line): Formula.Potential = {
    FormulaParser.parse(input) match {
      case \/-(f) => f
      case -\/(f: FormulaParser.ParseException) => fail(f.format)
      case -\/(f) => fail(f.toString)
    }
  }

  private def assertValidToTextAndBack(valid1: Formula.Valid)(implicit q: Line): Unit = {
    val text = Formula.Valid.toText(valid1, fieldSet)
    val potential = parseOrDie(text)
    Formula.Potential.validate(potential, fieldSet) match {
      case \/-(valid2) => assertEq(text, valid1, valid2)
      case -\/(f) => fail(s"Failed to reparse: $text\nError: ${f.value}")
    }
  }

  override def tests = Tests {

    "validToTextAndBack" - {

      "prop" - {
        for (valid <- RandomData.formula.valid.gen(fieldSet).samples())
          assertValidToTextAndBack(valid)
      }

      // "failure1" - {
      //   import Formula.Valid._
      //   val f = Add(Compare(Value(Bool(true)),!=,Field(NumberField(CustomField.Number.Id(1)))),Compare(Field(NumberField(CustomField.Number.Id(1))),>=,Field(NumberField(CustomField.Number.Id(2)))))
      //   assertValidToTextAndBack(f)
      // }

// >>>>>>> true <> field:score + field:score >= field:bonus
// + shipreq.webapp.member.project.formula.FormulaParserTest.fn.field.quoted 0ms  field:"Hot Dog"
// expect: Compare(Compare(Value(Bool(true)),!=,Add(Field(NumberField(CustomField.Number.Id(1))),Field(NumberField(CustomField.Number.Id(1))))),>=,Field(NumberField(CustomField.Number.Id(2))))
// actual: Add(Compare(Value(Bool(true)),!=,Field(NumberField(CustomField.Number.Id(1)))),Compare(Field(NumberField(CustomField.Number.Id(1))),>=,Field(NumberField(CustomField.Number.Id(2)))))
    }

  }
}
