package shipreq.webapp.client.project.app.pages.admin.access

import shipreq.webapp.base.data.ProjectPerm
import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.test._

object AccessPageTestDsl {

  final case class Ref(global   : TestGlobal,
                       confirmJs: TestConfirmJs)

  val * = Dsl[Ref, AccessPageObs, Unit]

  val global = new TestGlobal.TestDslWithObs(*)(_.global, _.global)

  val confirmJs = new TestConfirmJs.TestDsl(*)(_.confirmJs, _.confirmJs)

  val invariants: *.Invariants =
    *.emptyInvariant

  // ===================================================================================================================

  val leaveProjectButtonLoading = *.focus("Leave button loading").value(_.obs.leaveProjectButtonLoading)
  val existingUserRows          = *.focus("ExistingUser rows").collection(_.obs.existingUserRows.map(_.row))

  // ===================================================================================================================

  def existingUserSelect(rowIdx: Int, perm: ProjectPerm): *.Actions =
    *.action(s"Select '$perm' in dropdown in row $rowIdx")(_.obs.existingUserRows(rowIdx).dropdown.select(perm.toString))

  def existingUserClickSave(rowIdx: Int): *.Actions =
    *.action(s"Click 'Save' button in row $rowIdx")(_.obs.existingUserRows(rowIdx).saveButton.click())

  def existingUserClickDelete(rowIdx: Int): *.Actions =
    *.action(s"Click 'Delete' button in row $rowIdx")(_.obs.existingUserRows(rowIdx).deleteButton.click())

  val clickLeaveProject: *.Actions =
    *.action("Click 'Leave This Project'")(_.obs.leaveProjectButton.click())
}
