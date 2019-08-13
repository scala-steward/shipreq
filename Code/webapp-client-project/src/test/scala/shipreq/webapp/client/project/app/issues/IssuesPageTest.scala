package shipreq.webapp.client.project.app.issues

import utest._
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.test.SampleProject6
import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.app.ProjectSpaTestDsl
import shipreq.webapp.client.project.app.root.Routes.Page
import shipreq.webapp.client.project.test.PrepareEnv

object IssuesPageTest extends TestSuite {
  import IssuesPageTestDsl._

  PrepareEnv()

  private def runActions(project: Project)(a: *.Actions): Unit =
    runPlan(project)(Plan.action(a))

  private def runPlan(project: Project)(p: *.Plan): Unit = {
    import ProjectSpaTestDsl._

    ProjectSpaTestDsl.runTest(
      liftIssuePageTests(p).asAction(p.name.fold("Test")(_.value)),
      page = Page.Issues,
      project = project)
  }

  // TODO filters

  // IssueLite.DeadRefInReq(P6.frs(2), ReqTextLoc.Title, ContentRef.ReqRef(P6.mfs(28))),
  // IssueLite.DeadRefInReq(P6.uc1, ReqTextLoc.Title, ContentRef.UseCaseStepRef(16)),
  // IssueLite.DeadRefInReq(P6.uc1, ReqTextLoc.Title, ContentRef.UseCaseStepRef(17)),
  // IssueLite.BlankCustomField(P6.frs(1), P6.priField),
  // IssueLite.BlankCustomField(P6.frs(2), P6.priField),
  // IssueLite.BlankCustomField(P6.uc1, P6.priField),
  // IssueLite.BlankCustomField(P6.uc2, P6.priField),
  // IssueLite.BlankUseCaseStep(UseCaseStepId(18)),
  // IssueLite.BlankUseCaseStep(UseCaseStepId(19)),
  // IssueLite.IssueTagInReq(P6.frs(2), ReqTextLoc.Title, T.GenericReqTitle.Issue(2, SampleProject3.inlineIssueDesc)),
  // IssueLite.IssueTagInReq(P6.frs(1), ReqTextLoc.Title, T.GenericReqTitle.Issue(1, ∅)),

  override def tests = Tests {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    'edit - runActions(SampleProject6.project)(

      *.emptyAction
      +> rowCount.assert.equal(11)
      +> issueCategories.assert.equal(
        Some("Bad data (3)"),
        None,
        None,
        Some("Missing data (6)"),
        None,
        None,
        None,
        None,
        None,
        Some("User-defined (2)"),
        None)
      +> issueClasses.assert.equal(
        Some("Reference to deleted data (3)"),
        None,
        None,
        Some("Mandatory field is blank: Priority (4)"),
        None,
        None,
        None,
        Some("Use case step is blank (2)"),
        None,
        Some("#TBD"),
        Some("#TODO"))
      +> ids.assert.equal(
        Some("FR-2"),
        Some("UC-1"),
        None,
        Some("FR-1"),
        Some("FR-2"),
        Some("UC-1"),
        Some("UC-2"),
        Some("UC-1"),
        None,
        Some("FR-2"),
        Some("FR-1"))

      >> row(1).col(Column.FieldEditor).edit("[UC-1.0.X.1] and [UC-1.E.X.1] are dead. [UC-1.0.2.a] and [UC-1.E.1] are not." -> "#TODO", 2)
      +> rowCount.assert.equal(10)
      +> issueCategories.assert.equal(
        Some("Bad data"),
        Some("Missing data (6)"),
        None,
        None,
        None,
        None,
        None,
        Some("User-defined (3)"),
        None,
        None)
      +> issueClasses.assert.equal(
        Some("Reference to deleted data"),
        Some("Mandatory field is blank: Priority (4)"),
        None,
        None,
        None,
        Some("Use case step is blank (2)"),
        None,
        Some("#TBD"),
        Some("#TODO (2)"),
        None)

      >> row(9).col(Column.FieldEditor).edit("#TODO" -> "whatever")
      +> rowCount.assert.equal(9)
      +> issueCategories.assert.equal(
        Some("Bad data"),
        Some("Missing data (6)"),
        None,
        None,
        None,
        None,
        None,
        Some("User-defined (2)"),
        None)
      +> issueClasses.assert.equal(
        Some("Reference to deleted data"),
        Some("Mandatory field is blank: Priority (4)"),
        None,
        None,
        None,
        Some("Use case step is blank (2)"),
        None,
        Some("#TBD"),
        Some("#TODO"))
    )

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    'manual - runActions(SampleProject6.project)(

      newForm.edit("" -> "blah")
      +> rowCount.assert.equal(12)
      +> issueCategories.assert.equal(
        Some("Bad data (3)"),
        None,
        None,
        Some("Missing data (6)"),
        None,
        None,
        None,
        None,
        None,
        Some("User-defined (3)"),
        None,
        None)
      +> issueClasses.assert.equal(
        Some("Reference to deleted data (3)"),
        None,
        None,
        Some("Mandatory field is blank: Priority (4)"),
        None,
        None,
        None,
        Some("Use case step is blank (2)"),
        None,
        Some("#TBD"),
        Some("#TODO"),
        Some("Manual"))
      +> ids.assert.equal(
        Some("FR-2"),
        Some("UC-1"),
        None,
        Some("FR-1"),
        Some("FR-2"),
        Some("UC-1"),
        Some("UC-2"),
        Some("UC-1"),
        None,
        Some("FR-2"),
        Some("FR-1"),
        Some("–"))

      >> newForm.edit("" -> "another one!")
      +> rowCount.assert.equal(13)
      +> issueCategories.assert.equal(
        Some("Bad data (3)"),
        None,
        None,
        Some("Missing data (6)"),
        None,
        None,
        None,
        None,
        None,
        Some("User-defined (4)"),
        None,
        None,
        None)
      +> issueClasses.assert.equal(
        Some("Reference to deleted data (3)"),
        None,
        None,
        Some("Mandatory field is blank: Priority (4)"),
        None,
        None,
        None,
        Some("Use case step is blank (2)"),
        None,
        Some("#TBD"),
        Some("#TODO"),
        Some("Manual (2)"),
        None)
      +> ids.assert.equal(
        Some("FR-2"),
        Some("UC-1"),
        None,
        Some("FR-1"),
        Some("FR-2"),
        Some("UC-1"),
        Some("UC-2"),
        Some("UC-1"),
        None,
        Some("FR-2"),
        Some("FR-1"),
        Some("–"),
        Some("–"))

      >> row(11).col(Column.FieldEditor).edit("another one!" -> "a")
      +> rowCount.assert.equal(13)
      +> issueCategories.assert.equal(
        Some("Bad data (3)"),
        None,
        None,
        Some("Missing data (6)"),
        None,
        None,
        None,
        None,
        None,
        Some("User-defined (4)"),
        None,
        None,
        None)
      +> issueClasses.assert.equal(
        Some("Reference to deleted data (3)"),
        None,
        None,
        Some("Mandatory field is blank: Priority (4)"),
        None,
        None,
        None,
        Some("Use case step is blank (2)"),
        None,
        Some("#TBD"),
        Some("#TODO"),
        Some("Manual (2)"),
        None)
      +> ids.assert.equal(
        Some("FR-2"),
        Some("UC-1"),
        None,
        Some("FR-1"),
        Some("FR-2"),
        Some("UC-1"),
        Some("UC-2"),
        Some("UC-1"),
        None,
        Some("FR-2"),
        Some("FR-1"),
        Some("–"),
        Some("–"))

      >> row(11).col(Column.Actions).clickAction
      +> rowCount.assert.equal(12)
      +> issueCategories.assert.equal(
        Some("Bad data (3)"),
        None,
        None,
        Some("Missing data (6)"),
        None,
        None,
        None,
        None,
        None,
        Some("User-defined (3)"),
        None,
        None)
      +> issueClasses.assert.equal(
        Some("Reference to deleted data (3)"),
        None,
        None,
        Some("Mandatory field is blank: Priority (4)"),
        None,
        None,
        None,
        Some("Use case step is blank (2)"),
        None,
        Some("#TBD"),
        Some("#TODO"),
        Some("Manual"))
      +> ids.assert.equal(
        Some("FR-2"),
        Some("UC-1"),
        None,
        Some("FR-1"),
        Some("FR-2"),
        Some("UC-1"),
        Some("UC-2"),
        Some("UC-1"),
        None,
        Some("FR-2"),
        Some("FR-1"),
        Some("–"))
      +> row(11).col(Column.FieldEditor).text.assert.equal("blah")
    )

  }
}
