package shipreq.webapp.client.project.app.pages.admin.access

import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.app.ProjectSpaTestDsl
import shipreq.webapp.client.project.app.pages.root.Routes.Page
import shipreq.webapp.client.project.test.PrepareEnv
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.test.project.SampleProject
import utest._
import utest.framework.TestPath

object AccessPageTest extends TestSuite {
  import AccessPageTestDsl._

  PrepareEnv()

  private def runActions(project: Project)(a: *.Actions)(implicit tp: TestPath): Unit =
    runPlan(project)(Plan.action(a))

  private def runPlan(project: Project)(p: *.Plan)(implicit tp: TestPath): Unit = {
    import ProjectSpaTestDsl._

    val name = p.name.fold(tp.value.mkString("Test: ", ".", ""))(_.value)

    ProjectSpaTestDsl.runTest(
      liftAccessPageTests(p).asAction(name),
      page = Page.Access,
      project = project)
  }

  override def tests = Tests {

    "todo" - {
        runActions(SampleProject.project)(
          *.emptyAction
        )
    }
  }
}
