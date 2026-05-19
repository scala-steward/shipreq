package shipreq.webapp.client.project.app.pages.content.reqtable

import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.app.ProjectSpaTestDsl
import shipreq.webapp.client.project.app.pages.root.Routes.Page
import shipreq.webapp.client.project.test._
import shipreq.webapp.member.test.project.SampleProject.Values._
import shipreq.webapp.member.test.project.{SampleProject3, TestEvent}
import utest._
import utest.framework.TestPath

object TagRenameFilterTest extends TestSuite {
  import ReqTableTestDsl.{savedViews => _, _}
  import ReqTableTestDsl.savedViews.{* => _, _}

  PrepareEnv()

  def runTest(plan: *.PlanWithInitialState)(implicit path: TestPath): Unit = {
    import ProjectSpaTestDsl._
    ProjectSpaTestDsl.runTest(
      liftReqTableTests(plan.plan).asAction(path.value.mkString("TagRenameFilterTest.", ".", "")),
      page = Page.ReqTable,
      project = plan.initialState)
  }

  override def tests = Tests {

    "renameTagUpdatesFilter" - {
      val plan = Plan.action(
        enterFilter("#WIP")
          +> tablePubids.assert.equalIgnoringOrder("MF-5", "MF-6", "MF-7", "MF-12", "MF-13", "MF-22")
          +> filterText.assert("#WIP")
          >> receiveExternalEvent(TestEvent.applicableTagUpdate(wip, key = "DONE"))
          +> filterText.assert("#DONE")
          +> tablePubids.assert.equalIgnoringOrder("MF-5", "MF-6", "MF-7", "MF-12", "MF-13", "MF-22")
      ).withInitialState(SampleProject3.project)
      runTest(plan)
    }
  }
}
