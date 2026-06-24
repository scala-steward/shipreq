package shipreq.webapp.member.project.formula

import japgolly.microlibs.testutil.TestUtil._
import sourcecode.Line
import utest._

object FormulaParserTest extends TestSuite {
  import Formula.Potential._
  import FormulaCmpOp._

  private def parseOrDie(input: String)(implicit q: Line): Formula.Potential = {
    FormulaParser.parse(input) match {
      case \/-(f) => f
      case -\/(f: FormulaParser.ParseException) => fail(f.format)
      case -\/(f) => fail(f.toString)
    }
  }

  private def assertParse(input: String, expected: Formula.Potential)(implicit q: Line): String = {
    val f = parseOrDie(input)
    assertEq(input, f, expected)

    // Test FormulaAlgebra.unparse here too
    val text = toText(f)
    assertEq(text, parseOrDie(text), expected)
    text
  }

  override def tests = Tests {

    "literals" - {
      "empty" - assertParse("", value(FormulaValue.Empty))
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

    "fn" - {
      "0" - assertParse("now ( )", function("NOW", Nil))
      "1" - assertParse("not ( false )", function("NOT", lit(false) :: Nil))
      "if" - assertParse("IF ( 1=2 , \"good\" , 3*4 )", function("IF", List(
        compare(lit(1), `=`, lit(2)),
        lit("good"),
        multiply(lit(3), lit(4))
      )))

      "field" - {
        "direct" - assertParse("field:Rating", field("Rating"))
        "quoted" - assertParse("field:\"Hot Dog\"", field("Hot Dog"))
      }
    }

    "combos" - {
      "addMul" - assertParse("3 + 1 * 2", add(lit(3), multiply(lit(1), lit(2))))
      "mulAdd" - assertParse("1 * 2 + 3", add(multiply(lit(1), lit(2)), lit(3)))
      "mulMul" - assertParse("1 * 2 * 3", multiply(multiply(lit(1), lit(2)), lit(3)))
      "cmp" - assertParse("(1+6)*2 == 1+3*6", compare(multiply(add(lit(1), lit(6)), lit(2)), `=`, add(lit(1), multiply(lit(3), lit(6)))))
      "fnCmp" - assertParse("Round(1.2) > round(0.5)", compare(function("ROUND", lit(1.2) :: Nil), >, function("ROUND", lit(0.5) :: Nil)))
      "fnFieldAdd" - assertParse("round(field:Rating + 1)", function("ROUND", add(field("Rating"), lit(1)) :: Nil))
    }

  }
}
