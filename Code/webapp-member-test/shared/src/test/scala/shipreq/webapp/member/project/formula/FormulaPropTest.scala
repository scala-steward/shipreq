package shipreq.webapp.member.project.formula

import japgolly.microlibs.testutil.TestUtil._
import shipreq.webapp.base.test.BinaryTestUtil.assertRoundTripP
import shipreq.webapp.member.test.project.RandomData
import shipreq.webapp.member.project.protocol.binary.Latest.pickleValidFormula
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

    "valid" - {
      def samples() = RandomData.formula.valid.gen(fieldSet).samples().take(100)

      "toTextAndBack" - {
        for (valid <- samples())
          assertValidToTextAndBack(valid)
      }

      "toBinaryAndBack" - {
        for (valid <- samples())
          assertRoundTripP(valid)
      }

    }

  }
}
