package com.beardedlogic.usecase
package snippet

import scala.slick.jdbc.StaticQuery.interpolation
import net.liftweb.http.js.JsCmd
import org.scalatest.FunSpec
import org.scalatest.prop.PropertyChecks
import lib.{ExternalId, Defaults}
import lib.Types._
import db.{UseCaseRev, UseCaseSummary}
import test.TestDatabaseSupport
import util.ErrorMessages

class UseCaseIndexSnippetTest extends FunSpec with TestDatabaseSupport with PropertyChecks {
  import Tables._

  override def beforeEachWithDao {
    super.beforeEachWithDao

    // HACK BECAUSE OF OTHER TEMP HACK WHERE PROJECT_ID = 1
    dao.createProject(getOrCreateUserId(), "BLAH!")
    sqlu"update project set id=1".execute
  }

  describe("JSON generation") {
    it("should include tagged EIDs") {
      val ucs = UseCaseSummary("secret".tag[UseCaseIdentEITag], (3:Short).tag[UseCaseNumberTag], "hello", "now")
      val json = UseCaseIndex.toJson(ucs)
      json should include("secret")
    }
  }

  describe("#createNewUseCase") {
    def createNewUseCase: UseCaseSummary = assertTableDiffs(Usecase -> 1, UsecaseRev -> 1) {
      UseCaseIndex.create()
    }

    it("should create the first as \"1. Untitled\"") {
      truncate(Usecase)
      val uc = createNewUseCase
      uc.number should be(1)
      uc.title should be("Untitled")
    }

    // TODO New-UC has GLOBAL scope.

    it("should create the second as \"2. Untitled\"") {
      truncate(Usecase)
      createNewUseCase
      val uc = createNewUseCase
      uc.number should be(2)
      uc.title should be("Untitled")
    }
  }

  describe("#update") {
    def assertUpdateTriggered(js: JsCmd) {
      js.toJsCmd should (include(UseCaseIndex.TriggerUpdate.triggerName) and include("trigger"))
    }

    def assertUpdateNotTriggered(js: JsCmd) {
      js.toJsCmd should not include ("trigger")
    }

    def assertSummaryInAll(x: UseCaseSummary)(implicit projectId: ProjectId): Unit =
      dao.findAllUseCaseSummaries(projectId).map(ignoreTimestamp) should contain(ignoreTimestamp(x))

    def ignoreTimestamp(x: UseCaseSummary) = x.copy(updatedAt = "IGNORED")

    def newUc(implicit projectId: ProjectId) = dao.createUseCaseIdentAndRev1(projectId, Defaults.useCaseHeader)

    def params(id: UseCaseIdentId, newTitle: String) =
      Map("eid" -> ExternalId.UseCase(id), "title" -> newTitle)

    def test(params: Map[String, String]) = {
      withSessionParams(params) {
        val m = UseCaseIndex.update
        val js = UseCaseIndex.onUpdate(m)
        (m, js)
      }
    }

    def testSuccess(newTitle: String, expectedTitleAfterSave: String)(implicit projectId: ProjectId): UseCaseSummary =
      testSuccess2(newUc, newTitle, expectedTitleAfterSave)

    def testSuccess2(uc1: UseCaseRev, newTitle: String, expectedTitleAfterSave: String)(implicit projectId: ProjectId): UseCaseSummary = {
      val (r, js) = test(params(uc1, newTitle))
      r shouldBe defined
      val uc2 = r.openOrThrowException("required")
      assertJsErrorNotice(js, None)
      assertUpdateTriggered(js)
      uc2.number should equal(uc1.ident.number)
      uc2.title should equal(expectedTitleAfterSave)
      assertSummaryInAll(uc2)
      uc2
    }

    def testFailure(uc: UseCaseRev, errorMsgFrag: String, params: Map[String, String]) {
      val (r, js) = assertTableDiffs()(test(params))
      r shouldBe empty
      assertJsErrorNotice(js, Some(errorMsgFrag))
      assertUpdateNotTriggered(js)
      dao.findUseCaseRev(uc.id) should be(Some(uc))
    }

    it("should update new new UC") {
      implicit val project = newProjectId
      testSuccess("great", "great")
    }

    it("should correct invalid titles") {
      implicit val project = newProjectId
      val examples = Table(("INPUT", "OUTPUT")
        , ("   omg   ", "omg")
        , ("what     about", "what about")
        , ("what\tabout", "what about")
        , ("\tgreat  work\n", "great work")
        , ("", Defaults.title) // NOP actually
        , ("    ", Defaults.title) // NOP actually
      )
      forAll(examples)(testSuccess(_, _))
    }

    it("should appear to update when no change") {
      implicit val project = newProjectId
      val uc1 = newUc
      val uc2s = testSuccess2(uc1, "hello", "hello")
      val uc2 = dao.findUseCaseLatestRev(uc2s.parseId.get).get
      assertTableDiffs(){ testSuccess2(uc2, uc2.header.title, uc2.header.title) }
      assertSummaryInAll(uc2s)
    }

    it("should reject invalid input data") {
      implicit val project = newProjectId
      val uc = newUc
      testFailure(uc, "not found", params(98732156.tag[UseCaseIdentId], "hell0"))
      testFailure(uc, ErrorMessages.BadRequest, params(uc, "") - "title")
    }

    //it("should reject updates when UC rev not latest") {
    //  val uc = newUc
    //  val uc1 = db.updateUseCaseHeader(uc.identId, _.copy(title = "New Title!")).dataOpt.get // direct update (same valueId)
    //  val uc2 = db.updateUseCaseHeader(uc1.identId, _.copy(title = "Newer title")).dataOpt.get // audited update
    //  uc2.id should not be(uc.id)
    //  testFailure(uc2, ErrorMessages.StaleDataSubmitted, params(uc.identId, uc.id, "zxcvz"))
    //}
  }
}
