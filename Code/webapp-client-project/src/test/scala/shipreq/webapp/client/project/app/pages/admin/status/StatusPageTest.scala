package shipreq.webapp.client.project.app.pages.admin.status

import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.app.ProjectSpaTestDsl
import shipreq.webapp.client.project.app.pages.root.Routes.Page
import shipreq.webapp.client.project.test.PrepareEnv
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.test.project.SampleProject
import utest._
import utest.framework.TestPath

object StatusPageTest extends TestSuite {
  import StatusPageTestDsl._

  PrepareEnv()

  private def runActions(project: Project)(a: *.Actions)(implicit tp: TestPath): Unit =
    runPlan(project)(Plan.action(a))

  private def runPlan(project: Project)(p: *.Plan)(implicit tp: TestPath): Unit = {
    import ProjectSpaTestDsl._

    val name = p.name.fold(tp.value.mkString("Test: ", ".", ""))(_.value)

    ProjectSpaTestDsl.runTest(
      liftStatusPageTests(p).asAction(name),
      page = Page.Status,
      project = project)
  }

  override def tests = Tests {

    "loop" - runActions(SampleProject.project)(

      global.disableAutoResponse
        // Ready to delete
        +> deleteButton.exists.assert(true)
        +> cancelButton.exists.assert(false)
        +> restoreButton.exists.assert(false)
        +> messageExists.assert(false)
        +> tableExists.assert(true)
        +> textareaExists.assert(false)

      >> deleteButton.click
        +> deleteButton.exists.assert(true)
        +> cancelButton.exists.assert(true)
        +> restoreButton.exists.assert(false)
        +> tableExists.assert(false)
        +> textareaExists.assert(true)

      >> cancelButton.click
        // Ready to delete
        +> deleteButton.exists.assert(true)
        +> cancelButton.exists.assert(false)
        +> restoreButton.exists.assert(false)
        +> messageExists.assert(false)
        +> tableExists.assert(true)
        +> textareaExists.assert(false)

      >> deleteButton.click
        +> deleteButton.exists.assert(true)
        +> cancelButton.exists.assert(true)
        +> restoreButton.exists.assert(false)
        +> tableExists.assert(false)
        +> textareaExists.assert(true)

      >> deleteButton.click
        +> deleteButton.isLoading.assert(true)
        +> cancelButton.isDisabled.assert(true)
        +> restoreButton.exists.assert(false)

      >> global.autoRespondToLast
        // Project is now deleted
        +> deleteButton.exists.assert(false)
        +> cancelButton.exists.assert(false)
        +> restoreButton.exists.assert(true)
        +> messageExists.assert(true)
        +> tableExists.assert(true)
        +> textareaExists.assert(false)

      >> restoreButton.click
        +> deleteButton.exists.assert(false)
        +> cancelButton.exists.assert(false)
        +> restoreButton.isLoading.assert(true)

      >> global.autoRespondToLast
        // Project is now restored
        +> deleteButton.exists.assert(true)
        +> cancelButton.exists.assert(false)
        +> restoreButton.exists.assert(false)
        +> messageExists.assert(false)
        +> tableExists.assert(true)
        +> textareaExists.assert(false)
    )

  }
}
