package shipreq.webapp.client.project.app.pages.admin.status

import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.test._
import shipreq.webapp.member.test.CommonObs

object StatusPageTestDsl {

  final case class Ref(global: TestGlobal)

  val * = Dsl[Ref, StatusPageObs, Unit]

  val global = new TestGlobal.TestDslWithObs(*)(_.global, _.global)

  val invariants: *.Invariants =
    *.emptyInvariant

  // ===================================================================================================================

  val deleteButton  = new CommonObs.SemanticUiButton.TestDslOption(*, "delete")(_.deleteButton)
  val cancelButton  = new CommonObs.SemanticUiButton.TestDslOption(*, "cancel")(_.cancelButton)
  val restoreButton = new CommonObs.SemanticUiButton.TestDslOption(*, "restore")(_.restoreButton)

  val messageExists  = *.focus("message").value(_.obs.message.isDefined)
  val tableExists    = *.focus("table").value(_.obs.table.isDefined)
  val textareaExists = *.focus("textarea").value(_.obs.textarea.isDefined)
}
