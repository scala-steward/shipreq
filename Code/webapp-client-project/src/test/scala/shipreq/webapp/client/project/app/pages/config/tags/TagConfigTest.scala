package shipreq.webapp.client.project.app.pages.config.tags

import utest._
import utest.framework.TestPath
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.test.SampleProject6
import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.app.ProjectSpaTestDsl
import shipreq.webapp.client.project.app.pages.root.Routes.Page
import shipreq.webapp.client.project.test.PrepareEnv

object TagConfigTest extends TestSuite {
  import TagConfigTestDsl._

  PrepareEnv()

  private def runActions(project: Project)(a: *.Actions)(implicit tp: TestPath): Unit =
    runPlan(project)(Plan.action(a))

  private def runPlan(project: Project)(p: *.Plan)(implicit tp: TestPath): Unit = {
    import ProjectSpaTestDsl._

    val name = p.name.fold(tp.value.mkString("Test: ", ".", ""))(_.value)

    ProjectSpaTestDsl.runTest(
      liftTagConfigPageTests(p).asAction(name),
      page = Page.CfgTags,
      project = project)
  }

  override def tests = Tests {

    'init - runActions(SampleProject6.project)(
      *.emptyAction
    )

  }
}
