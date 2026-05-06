package shipreq.webapp.client.project.app.pages.admin.access

import shipreq.webapp.base.data.ProjectPerm._
import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.app.ProjectSpaTestDsl
import shipreq.webapp.client.project.app.pages.root.Routes.Page
import shipreq.webapp.client.project.test.PrepareEnv
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.event.Event
import shipreq.webapp.member.test.WebappTestUtil._
import shipreq.webapp.member.test.project.SampleProject
import utest._
import utest.framework.TestPath

object AccessPageTest extends TestSuite {
  import AccessPageTestDsl._
  import AccessPageObs._
  import AccessPageObs.ButtonStatus._

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

  private val project = applyEventsSuccessfully(SampleProject.project,
    Event.AccessUpdate(Map(
      PublicUserId2 -> Some(Admin),
      PublicUserId3 -> Some(Collaborator),
    )),
  )

  override def tests = Tests {
    // TODO test read-only mode

    "existing" - {

      "modifySelf" - runActions(project)(
        global.disableAutoResponse
        >> existingUserSelect(0, Collaborator)
        +> existingUserRows.assert(
          ExistingUserRow("You", Collaborator, Some(Enabled), None),
          ExistingUserRow(Username2.with_@, Admin, None, Some(Enabled)),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
        >> existingUserClickSave(0)
        +> confirmJs.calls.assert(0)
        +> existingUserRows.assert(
          ExistingUserRow("You", Collaborator, Some(Loading), None),
          ExistingUserRow(Username2.with_@, Admin, None, Some(Enabled)),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
        >> global.autoRespondToLast
        +> existingUserRows.assert(
          ExistingUserRow("You", Collaborator, None, None),
          ExistingUserRow(Username2.with_@, Admin, None, Some(Enabled)),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
      )

      "modifyOther" - runActions(project)(
        global.disableAutoResponse
        +> existingUserRows.assert(
          ExistingUserRow("You", Admin, None, None),
          ExistingUserRow(Username2.with_@, Admin, None, Some(Enabled)),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
        >> existingUserSelect(1, Collaborator)
        +> existingUserRows.assert(
          ExistingUserRow("You", Admin, None, None),
          ExistingUserRow(Username2.with_@, Collaborator, Some(Enabled), Some(Enabled)),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
        >> existingUserClickSave(1)
        +> confirmJs.calls.assert(0)
        +> existingUserRows.assert(
          ExistingUserRow("You", Admin, None, None),
          ExistingUserRow(Username2.with_@, Collaborator, Some(Loading), Some(Loading)),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
        >> global.autoRespondToLast
        +> existingUserRows.assert(
          ExistingUserRow("You", Admin, None, None),
          ExistingUserRow(Username2.with_@, Collaborator, None, Some(Enabled)),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
      )

      "delete" - runActions(project)(
        global.disableAutoResponse
        >> existingUserClickDelete(1)
        +> confirmJs.calls.assert(1)
        +> existingUserRows.assert(
          ExistingUserRow("You", Admin, None, None),
          ExistingUserRow(Username2.with_@, Admin, None, Some(Loading)),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
        >> global.autoRespondToLast
        +> existingUserRows.assert(
          ExistingUserRow("You", Admin, None, None),
          ExistingUserRow(Username3.with_@, Collaborator, None, Some(Enabled)),
        )
      )
    }

    "leave" - {

      "cancel" - runActions(project)(
        confirmJs.setNextResponse(false)
          >> clickLeaveProject
          +> confirmJs.calls.assert(1)
          +> leaveProjectButtonLoading.assert(false)
          +> global.requestCount.assert(0)
      )

      "confirm" - runActions(project)(
        global.disableAutoResponse
          >> confirmJs.setNextResponse(true)
          >> clickLeaveProject
          +> confirmJs.calls.assert(1)
          +> leaveProjectButtonLoading.assert(true)
          +> global.requestCount.assert(1)
          >> global.failLastRequest
          +> leaveProjectButtonLoading.assert(false)
      )
    }
  }
}
