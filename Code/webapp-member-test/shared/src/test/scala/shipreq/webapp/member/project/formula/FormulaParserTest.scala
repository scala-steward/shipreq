package shipreq.webapp.member.project.formula

import japgolly.microlibs.testutil.TestUtil._
import sourcecode.Line
import utest._

object FormulaParserTest extends TestSuite {
  import Formula.Potential._
  import FormulaCmpOp._

  private def assertParse(input: String, expected: Formula.Potential)(implicit q: Line): Unit = {
    FormulaParser.parse(input) match {
      case \/-(f) => assertEq(f, expected)
      case -\/(f: FormulaParser.ParseException) => fail(f.format)
      case -\/(f) => fail(f.toString)
    }
  }

  override def tests = Tests {

    "literals" - {
      "bool" - {
        "true" - assertParse("trUE", lit(true))
        "false" - assertParse("False", lit(false))
      }
      "str" - {
        "simple" - assertParse("\"ahh man\"", lit("ahh man"))
      }
      "double" - {
        "0" - assertParse("0", lit(0))
        "000" - assertParse("000", lit(0))
        "123" - assertParse("123", lit(123))
        "123.456" - assertParse("123.456", lit(123.456))
        "-123" - assertParse("-123", lit(-123))
        "-123.456" - assertParse("-123.456", lit(-123.456))
      }
    }

    "add" - assertParse("1+2", add(lit(1), lit(2)))
    "sub" - assertParse("1 - 2", subtract(lit(1), lit(2)))
    "mul" - assertParse("1 * 2", multiply(lit(1), lit(2)))
    "div" - assertParse("1/2", divide(lit(1), lit(2)))

    "parens" - {
      "addMul" - assertParse("3 + ( 1 * 2 )", add(lit(3), multiply(lit(1), lit(2))))
      "mulAdd" - assertParse("( 1 * 2 ) + 3", add(multiply(lit(1), lit(2)), lit(3)))
    }

    "cmp" - {
      "eq1" - assertParse("1=2", compare(lit(1), `=`, lit(2)))
      "eq2" - assertParse("1 == 2", compare(lit(1), `=`, lit(2)))
      "consec3" - assertParse("1 < 2 != false", compare(compare(lit(1), <, lit(2)), FormulaCmpOp.!=, lit(false)))
    }

    "combos" - {
      "addMul" - assertParse("3 + 1 * 2", add(lit(3), multiply(lit(1), lit(2))))
      "mulAdd" - assertParse("1 * 2 + 3", add(multiply(lit(1), lit(2)), lit(3)))
      "mulMul" - assertParse("1 * 2 * 3", multiply(multiply(lit(1), lit(2)), lit(3)))
      "cmp" - assertParse("(1+6)*2 == 1+3*6", compare(multiply(add(lit(1), lit(6)), lit(2)), `=`, add(lit(1), multiply(lit(3), lit(6)))))
    }

  }
}
