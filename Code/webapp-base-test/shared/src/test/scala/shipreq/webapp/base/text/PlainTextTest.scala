package shipreq.webapp.base.text

import shipreq.base.test.BaseTestUtil._
import shipreq.webapp.base.test.SampleProject6._
import shipreq.webapp.base.test.UnsafeTypes._
import utest._
import Values._

object PlainTextTest extends TestSuite {

  override def tests = TestSuite {
    'useCaseStepRefs {
      val full = "[UC-1.0.X.1] and [UC-1.E.X.1] are dead. [UC-1.0.2] and [UC-1.1.1] are not."

      'noCtx - assertEq(plainText              .reqTitleById(uc1), full)
      'uc1   - assertEq(plainText.withCtx(uc1 ).reqTitleById(uc1), full.replaceAll("UC-", ""))
      'uc2   - assertEq(plainText.withCtx(0.UC).reqTitleById(uc1), full)
    }
  }
}
