package shipreq.webapp.base.protocol.binary.v1

import nyaya.gen.Gen
import shipreq.base.test.BaseTestUtil._
import shipreq.webapp.base.{RandomData => R}
import shipreq.webapp.base.RandomData.TextGenExt
import shipreq.webapp.base.test.BinaryTestUtil.{propTestRoundTripP => test}
import shipreq.webapp.base.text.Text._
import shipreq.webapp.base.text.Text.Equality._
import utest._

object ProtocolTest extends TestSuite {
  import BaseData._
  import BaseMemberData1._
  import BaseMemberData2._
  import Events._
  import PostEvents._
  import AtomPicklers.instances._
  import ReqTableDataPicklers._

  private implicit def autoSomeG[A](g: Gen[A]): Option[Gen[A]] = Some(g)

  override def tests = Tests {

    'savedViews - test(R.project.flatMap(R.reqtableData.nonEmptySavedViewsForProject))

    'project - test(R.project)

    'text - {
      def gr = R.reqId
      def gu = R.useCaseStepId
      def gc = R.reqCode.id
      def gi = R.customIssueTypeId
      def ga = R.applicableTagId
      'CodeGroupTitle  - test(R.TextGen.codeGroupTitleAtom (gr, gu, gc, gi    ).text)
      'GenericReqTitle - test(R.TextGen.genericReqTitleAtom(gr, gu, gc, gi, ga).text)
      'InlineIssueDesc - test(R.TextGen.inlineIssueDescAtom(gr, gu, gc        ).text)
      'CustomTextField - test(R.TextGen.customTextFieldAtom(gr, gu, gc, gi, ga).text1(CustomTextField))
    }

  }
}
