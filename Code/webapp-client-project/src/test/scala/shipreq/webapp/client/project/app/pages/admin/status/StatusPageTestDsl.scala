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

  lazy val deleteButton  = new CommonObs.SemanticUiButton.TestDslOption(*, "delete")(_.deleteButton)
  lazy val cancelButton  = new CommonObs.SemanticUiButton.TestDslOption(*, "cancel")(_.cancelButton)
  lazy val restoreButton = new CommonObs.SemanticUiButton.TestDslOption(*, "restore")(_.restoreButton)
}
