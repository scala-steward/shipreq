package com.beardedlogic.usecase.integration

import org.openqa.selenium.Keys
import org.scalatest.{BeforeAndAfter, FunSuite}
import support.SeleniumTest
import com.beardedlogic.usecase.app.AppSiteMap
import com.beardedlogic.usecase.lib.Defaults
import com.beardedlogic.usecase.lib.Types._
import com.beardedlogic.usecase.test.TestDatabaseSupport
import AppSiteMap.Implicits._

class UseCaseIndexTest extends FunSuite with SeleniumTest with BeforeAndAfter with TestDatabaseSupport {

  override val wrapTestsInTransaction = false

  lazy val dsl = goto.useCaseIndex

  def assertDatabase(expected: (Int, String)*)(implicit projectId: ProjectId) {
    dao.summariseUseCases(projectId).map(s => (s.number.toInt, s.title)).toList should be(expected.toList)
  }

  def assertLinkUrl(implicit projectId: ProjectId) {
    val ucs = dao.summariseUseCases(projectId).head
    dsl.row(0).linkUrl should be(baseUrl + AppSiteMap.UseCaseEditor.relativeUrl(ucs.parseId.get))
  }

  test("empty initially") {
    dsl.assertItemCount(0)
  }

  test("adding UC") {
    implicit val project = newProjectId()
    dsl.clickNewUc().assertItemCount(1, 1).row(0).assertEditText(Defaults.title)
    assertDatabase((1, Defaults.title))
    10.times(keyboard.sendKeys(Keys.BACK_SPACE))
    keyboard.sendKeys("OMG\n")
    dsl.assertItemCount(1, 0).row(0).assertLinkText("UC-1: OMG")
    assertDatabase((1, "OMG"))
    assertLinkUrl
  }

  test("reloading page") {
    implicit val project = newProjectId()
    dsl.reload.assertItemCount(1, 0).row(0).assertLinkText("UC-1: OMG")
    assertLinkUrl
  }
}
