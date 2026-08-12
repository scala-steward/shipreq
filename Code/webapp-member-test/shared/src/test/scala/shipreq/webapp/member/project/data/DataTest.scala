package shipreq.webapp.member.project.data

import shipreq.base.util._
import shipreq.webapp.member.test.WebappTestUtil._
import shipreq.webapp.member.test.project.UnsafeTypes._
import utest._

object DataTest extends TestSuite {

  @inline def tr(a: Option[TagId], b: HashRefKey) = (a,b)
  @inline def ir(a: Option[CustomIssueTypeId], b: HashRefKey) = (a,b)

  val tagData = List(tr(1.AT, "abc"), tr(2.AT, "def"))
  val issueData = List(ir(1, "tbd"), ir(3, "todo"))

  override def tests = Tests {
    "validation" - {
      "hashRefKeyUniqueness" - {
        import DataValidators.hashRefKey._

        def test(input: String, expectedValidity: Validity, subjT: Option[TagId] = None, subjI: Option[CustomIssueTypeId] = None): Unit = {
          val state = State(SubState(subjT, () => tagData), SubState(subjI, () => issueData))
          assertEq(s"[$input] | $subjT, $subjI", hashRefKey(state).validity(input), expectedValidity)
        }

        "preventDups" - {
          test("hehe", Valid)
          test("abc", Invalid)
          test("todo", Invalid)
          test("   todo   ", Invalid)
        }

        "subjCanChangeItself" - {
          test("abc", Valid, subjT = 1.AT)
          test("abc", Invalid, subjT = 2.AT)
          test("todo", Valid, subjI = 3)
          test("todo", Invalid, subjI = 1)
        }

        "caseInsensitive" - {
          test("ABC", Invalid)
          test("ABCD", Valid)
          test("ABC", Valid, subjT = 1.AT)
          test("ABC", Invalid, subjT = 2.AT)
          test("ABC", Invalid, subjT = 3.AT)
        }
      }

      "noFormulaFieldCycles" - {
        import DataImplicits._
        import shipreq.webapp.member.project.formula.{Formula, FormulaParser, ValidFormula}

        val fmlAId = CustomField.Formula.Id(100)
        val fmlBId = CustomField.Formula.Id(101)
        val fmlCId = CustomField.Formula.Id(102)

        def dummyFormulaField(id: CustomField.Formula.Id, name: String): CustomField.Formula =
          CustomField.Formula(
            id = id,
            name = name,
            desc = None,
            decimalPlaces = 2,
            fieldReqTypeRules = FieldReqTypeRules.const(FieldReqTypeRules.Resolution.DefaultTo(
              ValidFormula(Formula.Potential.validate(FormulaParser.parse("10").toOption.get, FieldSet.empty).toOption.get)
            )),
            liveExplicitly = Live
          )

        val dummySet = FieldSet(
          emptyDataMap(CustomField).add(dummyFormulaField(fmlAId, "FmlA")).add(dummyFormulaField(fmlBId, "FmlB")).add(dummyFormulaField(fmlCId, "FmlC")),
          Vector(fmlAId, fmlBId, fmlCId)
        )

        def mkField(id: CustomField.Formula.Id, name: String, refExpr: String): CustomField.Formula = {
          val potential = FormulaParser.parse(refExpr).toOption.get
          val valid = Formula.Potential.validate(potential, dummySet).toOption.get
          CustomField.Formula(
            id = id,
            name = name,
            desc = None,
            decimalPlaces = 2,
            fieldReqTypeRules = FieldReqTypeRules.const(FieldReqTypeRules.Resolution.DefaultTo(ValidFormula(valid))),
            liveExplicitly = Live
          )
        }

        "acyclic" - {
          val fA = mkField(fmlAId, "FmlA", "field:FmlB + 1")
          val fB = mkField(fmlBId, "FmlB", "field:FmlC * 2")
          val fC = mkField(fmlCId, "FmlC", "100")
          val fs = FieldSet(emptyDataMap(CustomField).add(fA).add(fB).add(fC), Vector(fmlAId, fmlBId, fmlCId))

          assert(DataProp.fields.noFormulaFieldCycles(fs).success)
        }

        "cyclic" - {
          val fA = mkField(fmlAId, "FmlA", "field:FmlB + 1")
          val fB = mkField(fmlBId, "FmlB", "field:FmlC * 2")
          val fC = mkField(fmlCId, "FmlC", "field:FmlA + 5")
          val fs = FieldSet(emptyDataMap(CustomField).add(fA).add(fB).add(fC), Vector(fmlAId, fmlBId, fmlCId))

          assert(!DataProp.fields.noFormulaFieldCycles(fs).success)
        }

        "selfRef" - {
          val fA = mkField(fmlAId, "FmlA", "field:FmlA + 1")
          val fs = FieldSet(emptyDataMap(CustomField).add(fA), Vector(fmlAId))

          assert(!DataProp.fields.noFormulaFieldCycles(fs).success)
        }
      }
    }
  }
}