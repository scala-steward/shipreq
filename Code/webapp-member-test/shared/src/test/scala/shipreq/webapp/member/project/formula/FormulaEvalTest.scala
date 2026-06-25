package shipreq.webapp.member.project.formula

import japgolly.microlibs.testutil.TestUtil._
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.formula.FormulaValue._
import sourcecode.Line
import utest._

object FormulaEvalTest extends TestSuite {
  import FormulaTestShared._

  private def parseAndValidate(input: String, fieldSet: FieldSet): Formula.Valid = {
    FormulaParser.parse(input) match {
      case \/-(potential) =>
        Formula.Potential.validate(potential, fieldSet) match {
          case \/-(valid) => valid
          case -\/(err)   => fail(s"Validation failed: ${err.value}")
        }
      case -\/(err) => fail(s"Parsing failed: $err")
    }
  }

  private def assertEval(input: String,
                         expected: FormulaValue,
                         fieldSet: FieldSet = FieldSet.empty,
                         reqNums: ReqData.Numbers = ReqData.Numbers.empty,
                         req: Req = null)(implicit q: Line): Unit = {
    val formula = parseAndValidate(input, fieldSet)
    Formula.Valid.eval(formula, fieldSet, reqNums, req) match {
      case \/-(value) => assertEq(input, value, expected)
      case -\/(err)   => fail(s"Eval failed: ${err.value}")
    }
  }

  private def assertEvalError(input: String,
                              expectedErrorMsg: String,
                              fieldSet: FieldSet = FieldSet.empty,
                              reqNums: ReqData.Numbers = ReqData.Numbers.empty,
                              req: Req = null)(implicit q: Line): Unit = {
    val valid = parseAndValidate(input, fieldSet)
    Formula.Valid.eval(valid, fieldSet, reqNums, req) match {
      case \/-(value) => fail(s"Eval succeeded with $value, expected failure: $expectedErrorMsg")
      case -\/(err)   => assertEq(input, err.value, expectedErrorMsg)
    }
  }

  override def tests = Tests {

    "literals" - {
      "dbl" - assertEval("123.45", Dbl(123.45))
      "str" - assertEval("\"hello\"", Str("hello"))
      "boolTrue" - assertEval("true", Bool(true))
      "boolFalse" - assertEval("false", Bool(false))
    }

    "arithmetic" - {
      "addDbl" - assertEval("1 + 2", Dbl(3))
      "addStr" - assertEval("\"hello \" + \"world\"", Str("hello world"))
      "addStrDbl" - assertEval("\"number \" + 123", Str("number 123"))
      "addStrBool" - assertEval("\"bool is \" + true", Str("bool is TRUE"))
      "subDbl" - assertEval("5 - 2", Dbl(3))
      "mulDbl" - assertEval("3 * 4", Dbl(12))
      "divDbl" - assertEval("12 / 3", Dbl(4))
      "divZero" - assertEvalError("12 / 0", "Division by zero.")
      "addError" - assertEvalError("1 + \"a\"", "Type mismatch.")
      "subError" - assertEvalError("\"a\" - \"b\"", "Type mismatch.")
      "mulError" - assertEvalError("3 * \"a\"", "Type mismatch.")
      "divError" - assertEvalError("12 / \"a\"", "Type mismatch.")
    }

    "comparisons" - {
      "eqDblTrue" - assertEval("1 = 1", Bool(true))
      "eqDblFalse" - assertEval("1 = 2", Bool(false))
      "eqStrTrue" - assertEval("\"a\" = \"a\"", Bool(true))
      "eqStrFalse" - assertEval("\"a\" = \"b\"", Bool(false))
      "eqBoolTrue" - assertEval("true = true", Bool(true))
      "eqBoolFalse" - assertEval("true = false", Bool(false))
      "eqError" - assertEval("1 = \"a\"", Bool(false))

      "neDblTrue" - assertEval("1 != 2", Bool(true))
      "neDblFalse" - assertEval("1 != 1", Bool(false))
      "neStrTrue" - assertEval("\"a\" != \"b\"", Bool(true))
      "neBoolTrue" - assertEval("true != false", Bool(true))
      "neError" - assertEval("1 != \"a\"", Bool(true))

      "ltTrue" - assertEval("1 < 2", Bool(true))
      "ltFalse" - assertEval("2 < 1", Bool(false))
      "leTrue" - assertEval("1 <= 1", Bool(true))
      "gtTrue" - assertEval("2 > 1", Bool(true))
      "geTrue" - assertEval("2 >= 2", Bool(true))
      "compareStrError" - assertEvalError("\"a\" < \"b\"", "Type mismatch.")
      "compareBoolError" - assertEvalError("true < false", "Type mismatch.")

      "eqEmpty" - assertEval("IF(false, 1) = IF(false, 2)", Bool(true))
      "neEmpty" - assertEval("IF(false, 1) != IF(false, 2)", Bool(false))
    }

    "functions" - {
      "ifTrue" - assertEval("IF(true, 1)", Dbl(1))
      "ifFalse" - assertEval("IF(false, 1)", Empty)
      "ifElseTrue" - assertEval("IF(true, 1, 2)", Dbl(1))
      "ifElseFalse" - assertEval("IF(false, 1, 2)", Dbl(2))
      "ifError" - assertEvalError("IF(1, 2)", "Type mismatch.")

      "notTrue" - assertEval("NOT(true)", Bool(false))
      "notFalse" - assertEval("NOT(false)", Bool(true))
      "notError" - assertEvalError("NOT(1)", "Type mismatch.")

      "and0" - assertEval("AND()", Bool(true))
      "and1" - assertEval("AND(true)", Bool(true))
      "and2" - assertEval("AND(false)", Bool(false))
      "and3" - assertEval("AND(true, true)", Bool(true))
      "and4" - assertEval("AND(true, false)", Bool(false))
      "andError" - assertEvalError("AND(true, 1)", "Type mismatch.")

      "or0" - assertEval("OR()", Bool(false))
      "or1" - assertEval("OR(true)", Bool(true))
      "or2" - assertEval("OR(false)", Bool(false))
      "or3" - assertEval("OR(true, false)", Bool(true))
      "or4" - assertEval("OR(false, false)", Bool(false))
      "orError" - assertEvalError("OR(false, 1)", "Type mismatch.")

      "round1" - assertEval("ROUND(1.234)", Dbl(1))
      "round2" - assertEval("ROUND(1.5)", Dbl(2))
      "round3" - assertEval("ROUND(1.234, 2)", Dbl(1.23))
      "round4" - assertEval("ROUND(1.236, 2)", Dbl(1.24))
      "roundError" - assertEvalError("ROUND(\"a\")", "Type mismatch.")
      "roundScaleError" - assertEvalError("ROUND(1.2, \"a\")", "Type mismatch.")

      "average1" - assertEval("AVERAGE(2)", Dbl(2))
      "average2" - assertEval("AVERAGE(1, 2, 6)", Dbl(3))
      "average3" - assertEval("AVERAGE(1.5, 2.5)", Dbl(2))
      "averageError" - assertEvalError("AVERAGE(1, \"a\")", "Type mismatch.")

      "max1" - assertEval("MAX(5)", Dbl(5))
      "max2" - assertEval("MAX(1, 3, 2)", Dbl(3))
      "maxError" - assertEvalError("MAX(1, \"a\")", "Type mismatch.")

      "min1" - assertEval("MIN(5)", Dbl(5))
      "min2" - assertEval("MIN(3, 1, 2)", Dbl(1))
      "minError" - assertEvalError("MIN(1, \"a\")", "Type mismatch.")

      "ceiling1" - assertEval("CEILING(1.2)", Dbl(2))
      "ceiling2" - assertEval("CEILING(2.0)", Dbl(2))
      "ceiling3" - assertEval("CEILING(-1.2)", Dbl(-1))
      "ceilingError" - assertEvalError("CEILING(\"a\")", "Type mismatch.")

      "floor1" - assertEval("FLOOR(1.8)", Dbl(1))
      "floor2" - assertEval("FLOOR(2.0)", Dbl(2))
      "floor3" - assertEval("FLOOR(-1.2)", Dbl(-2))
      "floorError" - assertEvalError("FLOOR(\"a\")", "Type mismatch.")

      "isBlankTrue" - assertEval("ISBLANK(field:score)", Bool(true), fieldSet, req = req)
      "isBlankFalseDbl" - assertEval("ISBLANK(1)", Bool(false))
      "isBlankFalseStr" - assertEval("ISBLANK(\"hello\")", Bool(false))
      "isBlankFalseBool" - assertEval("ISBLANK(true)", Bool(false))

      "isBool1" - assertEval("ISBOOL(true)", Bool(true))
      "isBool2" - assertEval("ISBOOL(false)", Bool(true))
      "isBool3" - assertEval("ISBOOL(1.2)", Bool(false))
      "isBool4" - assertEval("ISBOOL(\"hello\")", Bool(false))

      "isNumber1" - assertEval("ISNUMBER(1.2)", Bool(true))
      "isNumber2" - assertEval("ISNUMBER(true)", Bool(false))
      "isNumber3" - assertEval("ISNUMBER(\"hello\")", Bool(false))

      "isText1" - assertEval("ISTEXT(\"hello\")", Bool(true))
      "isText2" - assertEval("ISTEXT(1.2)", Bool(false))
      "isText3" - assertEval("ISTEXT(true)", Bool(false))
    }

    "fields" - {
      "fieldPresent" - {
        val reqNums = ReqData.Numbers(Map(scoreFieldId -> Map(reqId -> 45.67)))
        assertEval("field:score", Dbl(45.67), fieldSet, reqNums, req)
      }
      "fieldDefault" - {
        assertEval("field:bonus", Dbl(10.0), fieldSet, ReqData.Numbers.empty, req)
      }
      "fieldEmpty" - {
        assertEval("field:score", Empty, fieldSet, ReqData.Numbers.empty, req)
      }
    }
  }
}
