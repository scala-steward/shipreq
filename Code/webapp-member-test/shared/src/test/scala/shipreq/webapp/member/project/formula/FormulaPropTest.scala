package shipreq.webapp.member.project.formula

import japgolly.microlibs.testutil.TestUtil._
import shipreq.webapp.member.test.project.RandomData
import sourcecode.Line
import utest._

object FormulaPropTest extends TestSuite {
  import FormulaTestShared._

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
      for (valid <- RandomData.formula.valid.gen(fieldSet).samples().take(100))
        assertValidToTextAndBack(valid)
    }

  }
}
